# 配置

以最简单的jdbc事务为例:

```xml
<!-- 数据源以Sping自带为例，每次请求均返回一个新的连接 -->
<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
	<property name="driverClassName" value="${jdbc.driverClassName}" />
	<property name="url" value="${jdbc.url}" />
    <property name="username" value="${jdbc.username}" />
    <property name="password" value="${jdbc.password}" />
</bean>
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
	<property name="dataSource" ref="dataSource"/>
</bean>
<tx:annotation-driven transaction-manager="transactionManager"/>
```

# 解析

TxNamespaceHandler.init:

```java
@Override
public void init() {
	registerBeanDefinitionParser("advice", new TxAdviceBeanDefinitionParser());
	registerBeanDefinitionParser("annotation-driven", 
        new AnnotationDrivenBeanDefinitionParser());
	registerBeanDefinitionParser("jta-transaction-manager", 										new JtaTransactionManagerBeanDefinitionParser());
}
```

明显解析的入口便在AnnotationDrivenBeanDefinitionParser.parse:

```java
@Override
public BeanDefinition parse(Element element, ParserContext parserContext) {
	registerTransactionalEventListenerFactory(parserContext);
	String mode = element.getAttribute("mode");
	if ("aspectj".equals(mode)) {
		// mode="aspectj"
		registerTransactionAspect(element, parserContext);
	} else {
		// mode="proxy"
		AopAutoProxyConfigurer.configureAutoProxyCreator(element, parserContext);
	}
	return null;
}
```

下面分部分进行说明。

##  TransactionalEventListener

第一部分用于向Spring容器注册TransactionalEventListener工厂，TransactionalEventListener是Spring4.2引入的新特性，允许我们自定义监听器监听事务的提交或其它动作。

## 主要组件注册

即configureAutoProxyCreator方法，此方法的最终作用便是在Spring容器中加入这样的bean结构:

BeanFactoryTransactionAttributeSourceAdvisor->TransactionInterceptor->AnnotationTransactionAttributeSource

其中AnnotationTransactionAttributeSource用于解析@Transactional注解的相关属性。

## 代理类生成

与aop模块类似，入口位于configureAutoProxyCreator里注册的bean: InfrastructureAdvisorAutoProxyCreator，其类图:

![InfrastructureAdvisorAutoProxyCreator类图](images/InfrastructureAdvisorAutoProxyCreator.jpg)

此类的特殊之处从其名字上可以体现: **只考虑Spring内部使用的基础设施Advisor**。

为类创建代理的入口位于AbstractAutoProxyCreator.postProcessAfterInitialization:

```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
	if (bean != null) {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		if (!this.earlyProxyReferences.contains(cacheKey)) {
			return wrapIfNecessary(bean, beanName, cacheKey);
		}
	}
	return bean;
}
```

wrapIfNecessary核心逻辑:

```java
protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
	// Create proxy if we have advice.
	Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
	if (specificInterceptors != DO_NOT_PROXY) {
		this.advisedBeans.put(cacheKey, Boolean.TRUE);
		Object proxy = createProxy(
			bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
		this.proxyTypes.put(cacheKey, proxy.getClass());
		return proxy;
	}
}
```

### Advisor寻找

getAdvicesAndAdvisorsForBean用于去容器中寻找适合当前bean的Advisor，其最终调用AbstractAdvisorAutoProxyCreator.findEligibleAdvisors:

```java
protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
	List<Advisor> candidateAdvisors = findCandidateAdvisors();
	List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
	extendAdvisors(eligibleAdvisors);
	if (!eligibleAdvisors.isEmpty()) {
      	//按照@Ordered排序
		eligibleAdvisors = sortAdvisors(eligibleAdvisors);
	}
	return eligibleAdvisors;
}
```

这个方法在spring-aop中已经详细说明过了，这里再强调一下具体的查找逻辑:

- 首先去容器找出所有实现了Advisor接口的bean，对应findCandidateAdvisors方法.
- 逐一判断Advisor是否适用于当前bean，对应findAdvisorsThatCanApply方法，判断逻辑为:
  - 如果Advisor是IntroductionAdvisor，那么判断其ClassFilter是否可以匹配bean的类.
  - 如果Advisor是PointcutAdvisor，那么首先进行ClassFilter匹配，如果匹配失败，那么再获得Advisor的MethodMatcher对象，如果MethodMatcher可以匹配任意方法，那么返回true，否则反射获取给定bean的所有方法逐一进行匹配，只要有一个匹配成功，即返回true.
  - 其它情况，直接返回true.

对于spring事务来说，我们有唯一的Advisor: BeanFactoryTransactionAttributeSourceAdvisor,其类图:

![BeanFactoryTransactionAttributeSourceAdvisor类图](images/BeanFactoryTransactionAttributeSourceAdvisor.jpg)

可以看出，BeanFactoryTransactionAttributeSourceAdvisor其实是一个PointcutAdvisor，所以**是否可以匹配取决于其Pointcut**。此Advisor的pointcut是一个TransactionAttributeSourcePointcut对象，类图:

![TransactionAttributeSourcePointcut类图](images/TransactionAttributeSourcePointcut.jpg)

**Pointcut的核心在于其ClassFilter和MethodMatcher**。

ClassFilter:

位于StaticMethodMatcherPointcut:

```java
private ClassFilter classFilter = ClassFilter.TRUE;
```

即: 类检查全部通过。

MethodMatcher:

TransactionAttributeSourcePointcut.matches:

```java
@Override
public boolean matches(Method method, Class<?> targetClass) {
  	//如果已经是事务代理，那么不应该再次代理
	if (TransactionalProxy.class.isAssignableFrom(targetClass)) {
		return false;
	}
	TransactionAttributeSource tas = getTransactionAttributeSource();
	return (tas == null || tas.getTransactionAttribute(method, targetClass) != null);
}
```

getTransactionAttribute方法使用了缓存的思想，但其核心逻辑位于AbstractFallbackTransactionAttributeSource.computeTransactionAttribute:

```java
protected TransactionAttribute computeTransactionAttribute(Method method, Class<?> targetClass) {
	// Don't allow no-public methods as required.
	if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
		return null;
	}
	// Ignore CGLIB subclasses - introspect the actual user class.
	Class<?> userClass = ClassUtils.getUserClass(targetClass);
	// The method may be on an interface, but we need attributes from the target class.
	// If the target class is null, the method will be unchanged.
	Method specificMethod = ClassUtils.getMostSpecificMethod(method, userClass);
	// If we are dealing with method with generic parameters, find the original method.
	specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
	// First try is the method in the target class.
	TransactionAttribute txAtt = findTransactionAttribute(specificMethod);
	if (txAtt != null) {
		return txAtt;
	}
	// Second try is the transaction attribute on the target class.
	txAtt = findTransactionAttribute(specificMethod.getDeclaringClass());
	if (txAtt != null && ClassUtils.isUserLevelMethod(method)) {
		return txAtt;
	}
	if (specificMethod != method) {
		// Fallback is to look at the original method.
		txAtt = findTransactionAttribute(method);
		if (txAtt != null) {
			return txAtt;
		}
		// Last fallback is the class of the original method.
		txAtt = findTransactionAttribute(method.getDeclaringClass());
		if (txAtt != null && ClassUtils.isUserLevelMethod(method)) {
			return txAtt;
		}
	}
	return null;
}
```

很明显可以看出，**首先去方法上查找是否有相应的事务注解(比如@Transactional)，如果没有，那么再去类上查找**。

# 运行

以JDK动态代理为例，JdkDynamicAopProxy.invoke简略版源码:

```java
@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
	if (chain.isEmpty()) {.
      	//没有可用的拦截器，直接调用原方法
		Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
		retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
	} else {
		// We need to create a method invocation...
		invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
		// Proceed to the joinpoint through the interceptor chain.
		Object retVal = invocation.proceed();
	}
}
```

## 调用链生成

即getInterceptorsAndDynamicInterceptionAdvice方法，其原理是:

**遍历所有使用的 Advisor，获得其Advice，将Advice转为MethodInterceptor**。那么是如何转的呢?

根据Spring的定义，Advice可以是一个MethodInterceptor，也可以是类似于Aspectj的before, after通知。转换由DefaultAdvisorAdapterRegistry.getInterceptors完成:

```java
@Override
public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
	List<MethodInterceptor> interceptors = new ArrayList<MethodInterceptor>(3);
	Advice advice = advisor.getAdvice();
	if (advice instanceof MethodInterceptor) {
		interceptors.add((MethodInterceptor) advice);
	}
	for (AdvisorAdapter adapter : this.adapters) {
		if (adapter.supportsAdvice(advice)) {
			interceptors.add(adapter.getInterceptor(advisor));
		}
	}
	if (interceptors.isEmpty()) {
		throw new UnknownAdviceTypeException(advisor.getAdvice());
	}
	return interceptors.toArray(new MethodInterceptor[interceptors.size()]);
}
```

AdvisorAdapter接口用以支持用户自定义的Advice类型，并将自定义的类型转为拦截器。默认adapters含有MethodBeforeAdviceAdapter、AfterReturningAdviceAdapter和ThrowsAdviceAdapter三种类型，用以分别支持MethodBeforeAdvice、AfterReturningAdvice和ThrowsAdvice。

**对于我们的BeanFactoryTransactionAttributeSourceAdvisor来说，有且只有一个拦截器: TransactionInterceptor**.

## 调用链调用

ReflectiveMethodInvocation.proceed:

```java
@Override
public Object proceed() throws Throwable {
	if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
      	//拦截器执行完毕，调用原本的方法
		return invokeJoinpoint();
	}
	Object interceptorOrInterceptionAdvice =
			this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
	if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
		InterceptorAndDynamicMethodMatcher dm =
				(InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
		if (dm.methodMatcher.matches(this.method, this.targetClass, this.arguments)) {
			return dm.interceptor.invoke(this);
		} else {
			// Dynamic matching failed.
			return proceed();
		}
	} else {
      	//调用拦截器的invoke方法
		return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
	}
}
```

可以看出，这其实是一个**逐个调用拦截器的invoke方法，最终调用原本方法(被代理方法)的过程**。所以，事务添加的核心逻辑(入口)在TransactionInterceptor的invoke方法。

## TransactionInterceptor

invoke方法:

```java
@Override
public Object invoke(final MethodInvocation invocation) throws Throwable {
	Class<?> targetClass = (invocation.getThis() != null ? 
             AopUtils.getTargetClass(invocation.getThis()) : null);
	// Adapt to TransactionAspectSupport's invokeWithinTransaction...
	return invokeWithinTransaction(invocation.getMethod(), targetClass, new InvocationCallback() {
		@Override
		public Object proceedWithInvocation() throws Throwable {
          	//事务执行完毕后调用链继续向下执行
			return invocation.proceed();
		}
	});
}
```

invokeWithinTransaction简略版源码(仅保留PlatformTransactionManager部分):

```java
protected Object invokeWithinTransaction(Method method, Class<?> targetClass, final InvocationCallback invocation){
	// If the transaction attribute is null, the method is non-transactional.
	final TransactionAttribute txAttr = getTransactionAttributeSource()
      	.getTransactionAttribute(method, targetClass);
	final PlatformTransactionManager tm = determineTransactionManager(txAttr);
	final String joinpointIdentification = methodIdentification(method, targetClass);
	if (txAttr == null || !(tm instanceof CallbackPreferringPlatformTransactionManager)) {
		// Standard transaction demarcation with getTransaction and commit/rollback calls.
		TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
		Object retVal = null;
		try {
			// This is an around advice: Invoke the next interceptor in the chain.
			// This will normally result in a target object being invoked.
			retVal = invocation.proceedWithInvocation();
		} catch (Throwable ex) {
			// target invocation exception
			completeTransactionAfterThrowing(txInfo, ex);
			throw ex;
		} finally {
			cleanupTransactionInfo(txInfo);
		}
		commitTransactionAfterReturning(txInfo);
		return retVal;
	}
}
```

### 事务管理器

determineTransactionManager方法用以确定使用的事务管理器:

```java
protected PlatformTransactionManager determineTransactionManager(TransactionAttribute txAttr) {
	//如果没有事务属性，那么仅从缓存中查找，找不到返回null
	if (txAttr == null || this.beanFactory == null) {
		return getTransactionManager();
	}
	String qualifier = txAttr.getQualifier();
  	//如果@Transactional注解配置了transactionManager或value属性(用以决定使用哪个事务管理器):
  	//首先查找缓存，找不到再去容器中按名称寻找
	if (StringUtils.hasText(qualifier)) {
		return determineQualifiedTransactionManager(qualifier);
	} else if (StringUtils.hasText(this.transactionManagerBeanName)) {
		return determineQualifiedTransactionManager(this.transactionManagerBeanName);
	} else {
      	//去容器中按类型(Class)查找
		PlatformTransactionManager defaultTransactionManager = getTransactionManager();
		if (defaultTransactionManager == null) {
			defaultTransactionManager = this.beanFactory.getBean(PlatformTransactionManager.class);
			this.transactionManagerCache.putIfAbsent(
					DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
		}
		return defaultTransactionManager;
	}
}
```

对于我们使用的DataSourceTransactionManager，类图:

![DataSourceTransactionManager](images/DataSourceTransactionManager.jpg)

afterPropertiesSet方法只是对dataSource进行了检查。

### DataSource

DriverManagerDataSource类图:

![DriverManagerDataSource类图](images/DriverManagerDataSource.jpg)

其中CommonDataSource、Wrapper、DataSource均位于javax.sql包下。