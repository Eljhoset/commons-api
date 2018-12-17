package com.eljhoset.controlleradvice.config;

import com.eljhoset.controlleradvice.ApiException;
import com.eljhoset.controlleradvice.DefaultExceptionResponseInterseptor;
import com.eljhoset.controlleradvice.ExceptionResponseInterseptor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import static java.util.stream.Collectors.groupingBy;

@Configuration
public class BeanRegisterConfig {
	@ConditionalOnMissingBean
	@Bean
	public ExceptionResponseInterseptor exceptionResponseInterseptor() {
		return new DefaultExceptionResponseInterseptor();
	}

	@Configuration
	public static class BeanDefinitionRegistryPostProcessorConfig {
		@Bean
		public static BeanDefinitionRegistryPostProcessor beanDefinitionRegistryPostProcessor(ExceptionResponseInterseptor exceptionResponseInterseptor, ApplicationContext applicationContext) {
			return new BeanDefinitionRegistryPostProcessor() {
				final ByteBuddy byteBuddy = new ByteBuddy();

				@Override
				public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

				}

				@Override
				public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
					final Object mainBean = applicationContext.getBean(applicationContext.getBeanNamesForAnnotation(SpringBootApplication.class)[0]);
					final String packageName = mainBean.getClass().getPackageName();
					final Reflections reflections = new Reflections(packageName);
					reflections.getTypesAnnotatedWith(ApiException.class).stream()
							.collect(groupingBy(c -> c.getAnnotation(ApiException.class).code()))
							.entrySet()
							.stream().filter(f -> f.getKey() > 1).findFirst().ifPresent(duplicated -> {
						throw new RuntimeException(String.format("Duplicate code %d found on api exceptions %s", duplicated.getKey(), duplicated.getValue()));
					});


					DynamicType.Builder<Object> subclass = byteBuddy.subclass(Object.class)
							.annotateType(AnnotationDescription.Builder.ofType(ControllerAdvice.class).build());

					AnnotationDescription exceptionHandler = AnnotationDescription.Builder.ofType(ExceptionHandler.class)
							.build();
					AnnotationDescription responseBody = AnnotationDescription.Builder.ofType(ResponseBody.class)
							.build();
					Class<?> beanClass = subclass
							.defineMethod("handle", Object.class, Visibility.PUBLIC)
							.withParameter(RuntimeException.class, "exception")
							.intercept(MethodDelegation.to(exceptionResponseInterseptor))
							.annotateMethod(exceptionHandler, responseBody)
							.make()
							.load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
							.getLoaded();
					RootBeanDefinition bean = new RootBeanDefinition(beanClass, Autowire.BY_TYPE.value(), true);
					beanDefinitionRegistry.registerBeanDefinition(beanClass.getName(), bean);
				}

			};
		}
	}


}
