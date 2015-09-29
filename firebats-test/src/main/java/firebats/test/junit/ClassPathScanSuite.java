package firebats.test.junit;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nullable;

import org.junit.experimental.categories.Categories.CategoryFilter;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

/**
 * 自动classpath查找Suite实现
 * 
 * 范例：
 * @RunWith(ClassPathSuite.class)
 * @IncludeCategory(SlowTestCategory.class)
 * @SuiteClasses(packageName="junit4")
 * public class AllSlowTests {
 * }
 * 
 * public interface SlowTestCategory {}
 * 
 * @Category(SlowTestCategory.class)
 * public class ASlowTest {
 *   	@Test	public void test() {}
 * }
 */
public class ClassPathScanSuite extends org.junit.runners.Suite {
    /**
     * The <code>SuiteClasses</code> annotation specifies the classes to be run when a class
     * annotated with <code>@RunWith(Suite.class)</code> is run.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface SuiteClasses {
        /**
         * @return the classes to be run
         */
        public String packageName();
    }

	static public ClassPath classpath;
	static {
		try {
			classpath = ClassPath.from(Thread.currentThread().getContextClassLoader());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * for junit invoke.
	 */
	public ClassPathScanSuite(Class<?> clazz, RunnerBuilder builder)
			throws InitializationError {
		super(builder, clazz, Iterables.toArray(getClasses(getPackageName(clazz), getIncludedCategory(clazz)), Class.class));
		CategoryFilter filter=new CategoryFilter(getIncludedCategory(clazz),getExcludedCategory(clazz));
 		try {
 	 		filter(new ClassPathCategoryFilter(getPackageName(clazz),filter));
		} catch (NoTestsRemainException e) {
            throw new InitializationError(e);
		}
	}
	
	public static class ClassPathCategoryFilter extends Filter {
  		private String packageName;
  		CategoryFilter filter;
 		public ClassPathCategoryFilter(String packageName,CategoryFilter filter) {
			this.filter=filter;
 			this.packageName=packageName;
  		}
		@Override
		public boolean shouldRun(Description description) {
			if(description!=null&&description.getTestClass()!=null){
				return description.getTestClass().getPackage().getName().startsWith(packageName)
						&&filter.shouldRun(description);
			}
 			return false;
		}
		@Override
		public String describe() { 
            return filter.describe() +" ,packages " +packageName;
		}
	}

	private static ImmutableSet<Class<?>> getClasses(String packageName,
			final Class<?> annotationClass) {
		return FluentIterable
				.from(classpath.getTopLevelClassesRecursive(packageName))
				.filter(new Predicate<ClassPath.ClassInfo>() {
					@Override
					public boolean apply(ClassInfo input) {
						return true;
					}
				})
				.transform(new Function<ClassInfo, Class<?>>() {
					@Override
					@Nullable
					public Class<?> apply(@Nullable ClassInfo input) {
						try {
							return Class.forName(input.getName());
						} catch (ClassNotFoundException e) {
							throw new RuntimeException(e);
						}
					}
				})
				.filter(new Predicate<Class<?>>() {
					@Override
					public boolean apply(@Nullable Class<?> input) {
						return true;
					}
				}).toSet();
	}
	
	private static Class<?> getExcludedCategory(Class<?> klass) {
		ExcludeCategory annotation= klass.getAnnotation(ExcludeCategory.class);
		return annotation == null ? null : annotation.value();
	}
	private static Class<?> getIncludedCategory(Class<?> klass) {
		IncludeCategory annotation= klass.getAnnotation(IncludeCategory.class);
		return annotation == null ? null : annotation.value();
	}
	private static String getPackageName(Class<?> klass) {
		SuiteClasses annotation= klass.getAnnotation(SuiteClasses.class);
		return annotation == null ? "no package name" : annotation.packageName();
	}
}
