package firebats.reflect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import javax.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.reflect.TypeToken;


/**
 * chenpeng : copy from guava
 * <p/>
 * 为什么不直接使用guava的TypeToken？ 原因如下：
 * <ul>
 * <li>guava的TypeToken 实现了Serializable,每次都Warning: The serializable class
 * does not declare a static final serialVersionUID field of type long</li>
 * <li>可以提供一个中转类型，在jeckson,fastjson,之间互转</li>
 * </ul>
 * chenpeng end
 * <p/>
 * 用法同guava库，详细请参考：
 * 
 * @see com.google.common.reflect.TypeToken
 */
@Beta
public abstract class TypeRef<T> {

	private final TypeToken<T> runtimeType;

	private TypeRef(TypeToken<T> runtimeType) {
		this.runtimeType = checkNotNull(runtimeType);
	}

	/** Returns the captured type. */
	private final Type capture() {
		Type superclass = getClass().getGenericSuperclass();
		checkArgument(superclass instanceof ParameterizedType,
				"%s isn't parameterized", superclass);
		return ((ParameterizedType) superclass).getActualTypeArguments()[0];
	}

	@SuppressWarnings("unchecked")
	protected TypeRef() {
		Type t = capture();
		checkState(
				!(t instanceof TypeVariable),
				"Cannot construct a TypeToken for a type variable.\n"
						+ "You probably meant to call new TypeToken<%s>(getClass()) "
						+ "that can resolve the type variable for you.\n"
						+ "If you do need to create a TypeToken of a type variable, "
						+ "please use TypeToken.of() instead.", t);
		runtimeType = (TypeToken<T>) TypeToken.of(t);
	}

	/** Returns the represented type. */
	public final TypeToken<T> getGuavaTypeToken() {
		return runtimeType;
	}

	public final <X> TypeRef<T> where(TypeParameter<X> typeParam,
			TypeRef<X> typeArg) {
		runtimeType.where(typeParam, typeArg.runtimeType);
		return new SimpleTypeRef<T>(runtimeType.where(typeParam,
				typeArg.runtimeType));
	}

	public final <X> TypeRef<T> where(TypeParameter<X> typeParam,
			Class<X> typeArg) {
		return where(typeParam, of(typeArg));
	}

	/** Returns an instance of type token that wraps {@code type}. */
	public static <T> TypeRef<T> of(Class<T> type) {
		return new SimpleTypeRef<T>(TypeToken.of(type));
	}
	private static final class SimpleTypeRef<T> extends TypeRef<T> {
		SimpleTypeRef(TypeToken<T> runtimeType) {
			super(runtimeType);
		}

		@SuppressWarnings("unused")
		private static final long serialVersionUID = 0;
	}
	
	  /** Returns the represented type. */
	  public final Type getType() {
	    return runtimeType.getType();
	  }

	  /**
	   * Returns true if {@code o} is another {@code TypeRef} that represents the same {@link TypeToken}.
	   */
	  @Override public boolean equals(@Nullable Object o) {
	    if (o instanceof TypeToken) {
	    	TypeRef<?> that = (TypeRef<?>) o;
	      return runtimeType.equals(that.runtimeType);
	    }
	    return false;
	  }

	  @Override public int hashCode() {
	    return runtimeType.hashCode();
	  }
	  
	  @Override public String toString() {
		    return runtimeType.toString();
      }


	/**
	 * chenpeng : copy from guava
	 * 
	 * <p/>
	 * 用法同guava库，详细请参考：
	 * 
	 * @see com.google.common.reflect.TypeParameter
	 */
	@Beta
	public static abstract class TypeParameter<P> extends
			com.google.common.reflect.TypeParameter<P> {
	}

	public boolean isAssignableFrom(Type type) {
		return runtimeType.isAssignableFrom(type);
	}
	public boolean isAssignableFrom(TypeRef<?> type) {
		return runtimeType.isAssignableFrom(type.runtimeType);
	}

	
}