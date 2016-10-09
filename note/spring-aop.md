# 开头

aop部分的解析器由AopNamespaceHandler注册，其init方法:

```java
@Override
public void init() {
	registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());
	registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());
	registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());
}
```

# aop:config

此标签用以配置pointcut, advisor, aspect，实例:

```xml
<aop:config>
	<aop:pointcut expression="execution(* exam.service..*.*(..))" id="transaction"/>
	<aop:advisor advice-ref="txAdvice" pointcut-ref="transaction"/>
  	<aop:aspect ref="" />
</aop:config>
```

ConfigBeanDefinitionParser.parse:

```java
@Override
public BeanDefinition parse(Element element, ParserContext parserContext) {
	CompositeComponentDefinition compositeDef =
			new CompositeComponentDefinition(element.getTagName(), 
			parserContext.extractSource(element));
	parserContext.pushContainingComponent(compositeDef);
  	// 是否生成代理类
	configureAutoProxyCreator(parserContext, element);
	List<Element> childElts = DomUtils.getChildElements(element);
	for (Element elt: childElts) {
		String localName = parserContext.getDelegate().getLocalName(elt);
		if (POINTCUT.equals(localName)) {
			parsePointcut(elt, parserContext);
		} else if (ADVISOR.equals(localName)) {
			parseAdvisor(elt, parserContext);
		} else if (ASPECT.equals(localName)) {
			parseAspect(elt, parserContext);
		}
	}
	parserContext.popAndRegisterContainingComponent();
	return null;
}
```

## 解析

解析的过程主要分为以下几个部分。

### proxy-target-class & expose-proxy

对应着aop:config的两个属性，前者代表是否为被代理这生成CGLIB子类，默认false，只为接口生成代理子类(话说如果不生成子类那么怎么拦截?)。后者代表是否将代理bean暴露给用户，如果暴露，可以通过Spring AopContext类获得，默认不暴露。

解析的过程无非就是属性的读取，不再详细说明。

### aop:pointcut

pointcut的解析是一个生成一个BeanDefinition并将其id, expression等属性保存在BeanDefinition中。注意以下几点:

- BeanDefinition的ID来自于id属性，如果没有，那么自动生成。
- BeanDefinition的class是AspectJExpressionPointcut。
- BeanDefinition的scope为prototype。

AspectJExpressionPointcut类图:

![AspectJExpressionPointcut类图](images/AspectJExpressionPointcut.jpg)

### aop:advisor

首先是其所有属性的示例:

```xml
<aop:advisor id="" order="" advice-ref="aopAdvice" pointcut="" pointcut-ref="" />
```

advisor概念是Spring独有的，来自于上古时代，应该是较早时候的aop概念的实现: [AOP Alliance (Java/J2EE AOP standards)](http://aopalliance.sourceforge.net/)。Spring官方的说法: [aop-schema-advisors](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/aop.html#aop-schema-advisors)。

其相关的包/类就在spring-aop下:

![aopalliance包](images/aopalliance.png)

advice-ref是必须的属性，**并且这里的advice必须实现org.aopalliance.aop.Advice的子接口**。这些子接口指的什么呢，见Spring官方文档: [aop-api-advice-types](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/aop-api.html#aop-api-advice-types)。比如org.aopalliance.intercept.MethodInterceptor。

最常见的用途就是结合事务使用:

```xml
<tx:advice id="txAdvice" transaction-manager="transactionManager">
	<tx:attributes>
		<tx:method name="get*" read-only="true" propagation="NOT_SUPPORTED"/>
		<tx:method name="find*" read-only="true" propagation="NOT_SUPPORTED"/>
		<tx:method name="*" propagation="REQUIRED"/>
	</tx:attributes>
</tx:advice>

<aop:config>
	<aop:pointcut expression="execution(* exam.service..*.*(..))" id="transaction"/>
	<aop:advisor advice-ref="txAdvice" pointcut-ref="transaction"/>
</aop:config>
```

解析的套路和楼上类似，只不过此处的beanClass是DefaultBeanFactoryPointcutAdvisor，其类图:

![DefaultBeanFactoryPointcutAdvisor类图](images/DefaultBeanFactoryPointcutAdvisor.jpg)

另外注意对于pointcut和pointcut-ref两者处理的区别，对于pointcut属性，Spring会同样创建一个AspectJExpressionPointcut类型的BeanDefinition，对于pointcut-ref会生成一个RuntimeBeanReference对象指向原pointcut的引用。此类的类图:

![RuntimeBeanReference类图](images/RuntimeBeanReference.jpg)

可以看出，这种aop的实现需要实现各种接口，所以不应该再使用此种方式进行aop，除了Spring内部的实现。

### aop:aspect

配置举例:

```xml
<bean id="aopAdvice" class="base.aop.AopDemoAdvice" />
<!-- 必须配置，因为被代理的对象必须在Spring容器中 -->
<bean id="aopDemo" class="base.aop.AopDemo" />
<aop:config>
	<aop:pointcut id="pointcut" expression="execution(* base.aop.AopDemo.send())" />
	<aop:aspect ref="aopAdvice">
		<aop:before method="beforeSend" pointcut-ref="pointcut" />
		<aop:after method="afterSend" pointcut-ref="pointcut" />
	</aop:aspect>
</aop:config>
```

解析形成的BeanDefinition结构如下:

```html
AspectComponentDefinition
	beanRefArray
		RuntimeBeanReference(aop:aspect的ref属性)
	beanDefArray
		// 被注册
		RootBeanDefinition(aop:declare-parents)
			beanClass: DeclareParentsAdvisor
			ConstructorArg
				implement-interface
				types-matching
				default-impl
				delegate-ref
		// 被注册
		RootBeanDefinition(aop:before,aop:after...)
			beanClass: AspectJPointcutAdvisor
			ConstructorArg
				RootBeanDefinition
					beanClass: 由子标签决定
					ConstructorArg
						RootBeanDefinition
							beanClass: MethodLocatingFactoryBean
							properties
								targetBeanName: aspectName
								methodName: method属性
						RootBeanDefinition
							beanClass: SimpleBeanFactoryAwareAspectInstanceFactory
							properties
								aspectBeanName: aspectName
						//还有pointcut定义和引用...
```

结构图里面的aspectName来自于aop:aspect的ref属性，此属性是必须配置的，因为Spring要知道aop:before等标签指定的方法是哪个bean/类/对象的方法。

#### aop:declare-parents

对于aop:declare-parents子标签，其决定的是代理子类应该实现哪些接口:

```xml
<aop:declare-parents types-matching="" implement-interface="" />
```

此标签最终被解析成为beanClass为DeclareParentsAdvisor的BeanDefinition，并注册到容器中。其类图:

![DeclareParentsAdvisor类图](images/DeclareParentsAdvisor.jpg)

#### 其它

此处的其它指的是aop:before, aop:after等最核心的标签。其最终被解析为beanClass为AspectJPointcutAdvisor的BeanDefinition，类图:

![AspectJPointcutAdvisor类图](images/AspectJPointcutAdvisor.jpg)

正如上面结构图里所描述的，其构造参数为一个BeanDefintion，此对象的beanClass是不确定的，由aop:before/after中的before和after决定，代码:

```java
private Class<?> getAdviceClass(Element adviceElement, ParserContext parserContext) {
	String elementName = parserContext.getDelegate().getLocalName(adviceElement);
	if (BEFORE.equals(elementName)) {
		return AspectJMethodBeforeAdvice.class;
	} else if (AFTER.equals(elementName)) {
		return AspectJAfterAdvice.class;
	} else if (AFTER_RETURNING_ELEMENT.equals(elementName)) {
		return AspectJAfterReturningAdvice.class;
    } else if (AFTER_THROWING_ELEMENT.equals(elementName)) {
		return AspectJAfterThrowingAdvice.class;
	} else if (AROUND.equals(elementName)) {
		return AspectJAroundAdvice.class;
	}
}
```

而此BeanDefintion的构造参数又由以下三个部分组成:

##### MethodLocatingFactoryBean

第一个便是beanClass为此类型的BeanDefinition。其内部有一个methodName属性，存储的便是标签的method属性的值。其类图:

![MethodLocatingFactoryBean类图](images/MethodLocatingFactoryBean.jpg)

这个东西是干什么用的呢?其实是用于在指定的advice(aop:aspect的ref属性)中得到Method对象。入口在setBeanFactory方法:

```java
@Override
public void setBeanFactory(BeanFactory beanFactory) {
	Class<?> beanClass = beanFactory.getType(this.targetBeanName);
	this.method = BeanUtils.resolveSignature(this.methodName, beanClass);
}
```

##### SimpleBeanFactoryAwareAspectInstanceFactory

其类图:

![SimpleBeanFactoryAwareAspectInstanceFactory类图](images/SimpleBeanFactoryAwareAspectInstanceFactory.jpg)

此类用于在BeanFactory中定位aspect bean，这个bean指的是谁?

```xml
<bean id="aopAdvice" class="base.aop.AopDemoAdvice" />
```

就是它!查找很简单:

```java
@Override
public Object getAspectInstance() {
	return this.beanFactory.getBean(this.aspectBeanName);
}
```

#### 总结

从整个aop:aspect标签最终被解析为一个AspectJPointcutAdvisor来看，Spring在实现上仍将其作为Advisor的概念。



