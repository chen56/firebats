package firebats.reflect;


import static java.util.Arrays.asList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;

/**
 * Class-related utility  methods
 */
public final class Classes
{
    /**
     * Get all interfaces for the given type,
     * including the provided type. No type
     * is included twice in the list.
     *
     * @param type to extract interfaces from
     *
     * @return set of interfaces of given type
     */
    public static Set<Class<?>> getInterfacesOf( Type type )
    {
        Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
        addInterfaces( type, interfaces );

        if( type instanceof Class )
        {
            Class<?> current = (Class<?>) type;
            while( current != null )
            {
                addInterfaces( current, interfaces );
                current = current.getSuperclass();
            }
        }

        return interfaces;
    }

    /**
     * Get all interfaces for the given type,
     * including the provided type. No type
     * is included twice in the list.
     *
     * @param type to extract interfaces from
     *
     * @return set of interfaces of given type
     */
    public static Set<Type> getGenericInterfacesOf( Type type )
    {
        Set<Type> interfaces = new LinkedHashSet<Type>();
        addGenericInterfaces( type, interfaces );

        if( type instanceof Class )
        {
            Class<?> current = (Class<?>) type;
            while( current != null )
            {
                addGenericInterfaces( current, interfaces );
                current = current.getSuperclass();
            }
        }

        return interfaces;
    }

    public static Set<Class<?>> getInterfacesWithMethods( Set<Class<?>> interfaces )
    {
        Set<Class<?>> newSet = new LinkedHashSet<Class<?>>();
        for( Class<?> type : interfaces )
        {
            if( type.isInterface() && type.getDeclaredMethods().length > 0 )
            {
                newSet.add( type );
            }
        }

        return newSet;
    }

    public static Set<Class<?>> getClassesOf( Type type )
    {
        Set<Class<?>> types = new LinkedHashSet<Class<?>>();
        addInterfaces( type, types );

        if( type instanceof Class )
        {
            Class<?> current = (Class<?>) type;
            while( current != null )
            {
                types.add( current );
                current = current.getSuperclass();
            }
        }

        return types;
    }
    public static Set<Class<?>> getSuperClassesOf( Type type )
    {
        Set<Class<?>> types = new LinkedHashSet<Class<?>>();
        if( type instanceof Class )
        {
            Class<?> current = (Class<?>) type;
            while( current != null )
            {
                types.add( current );
                current = current.getSuperclass();
            }
        }
        return types;
    }

    public static Set<Class<?>> getTypesOf( Type type )
    {
        Set<Class<?>> types = new LinkedHashSet<Class<?>>();
        addInterfaces( type, types );

        if( type instanceof Class )
        {
            Class<?> current = (Class<?>) type;
            while( current != null )
            {
                addInterfaces( current, types );
                types.add( current );
                current = current.getSuperclass();
            }
        }

        return types;
    }

    public static Class<?>[] toClassArray( Set<Class<?>> types )
    {
        Class<?>[] array = new Class[types.size()];
        int idx = 0;
        for( Class<?> type : types )
        {
            array[ idx++ ] = type;
        }

        return array;
    }

    public static Type getActualTypeOf( Type type )
    {
        Set<Class<?>> types = getInterfacesOf( type );
        for( Type type1 : types )
        {
            if( type1 instanceof ParameterizedType )
            {
                return ( (ParameterizedType) type1 ).getActualTypeArguments()[ 0 ];
            }
        }
        return null;
    }

    public static String getSimpleGenericName( Type type )
    {
        if( type instanceof Class )
        {
            return ( (Class<?>) type ).getSimpleName();
        }
        else if( type instanceof ParameterizedType )
        {
            ParameterizedType pt = (ParameterizedType) type;
            String str = getSimpleGenericName( pt.getRawType() );
            str += "<";
            String args = "";
            for( Type typeArgument : pt.getActualTypeArguments() )
            {
                if( args.length() != 0 )
                {
                    args += ", ";
                }
                args += getSimpleGenericName( typeArgument );
            }
            str += args;
            str += ">";
            return str;
        }
        else if( type instanceof GenericArrayType )
        {
            GenericArrayType gat = (GenericArrayType) type;
            return getSimpleGenericName( gat.getGenericComponentType() ) + "[]";
        }
        else if( type instanceof TypeVariable )
        {
            TypeVariable<?> tv = (TypeVariable<?>) type;
            return tv.getName();
        }
        else if( type instanceof WildcardType )
        {
            WildcardType wt = (WildcardType) type;
            String args = "";
            for( Type typeArgument : wt.getUpperBounds() )
            {
                if( args.length() != 0 )
                {
                    args += ", ";
                }
                args += getSimpleGenericName( typeArgument );
            }

            return "? extends " + args;
        }
        else
        {
            throw new IllegalArgumentException( "Don't know how to deal with type:" + type );
        }
    }

    public static Class<?> getRawClass( final Type genericType )
    {
        // Calculate raw type
        if( genericType instanceof Class )
        {
            return (Class<?>) genericType;
        }
        else if( genericType instanceof ParameterizedType )
        {
            return (Class<?>) ( (ParameterizedType) genericType ).getRawType();
        }
        else if( genericType instanceof TypeVariable )
        {
            return (Class<?>) ( (TypeVariable<?>) genericType ).getGenericDeclaration();
        }
        else if( genericType instanceof WildcardType )
        {
            return (Class<?>) ( (WildcardType) genericType ).getUpperBounds()[ 0 ];
        }
        else if( genericType instanceof GenericArrayType )
        {
            Object temp = Array.newInstance( (Class<?>) ( (GenericArrayType) genericType ).getGenericComponentType(), 0 );
            return temp.getClass();
        }
        throw new IllegalArgumentException( "Could not extract the raw class of " + genericType );
    }

    public static List<Constructor<?>> getConstructorsOf( Class<?> clazz )
    {
        List<Constructor<?>> constructors = new ArrayList<Constructor<?>>();
        addConstructors( clazz, constructors );
        return constructors;
    }

    private static void addConstructors( Class<?> clazz, List<Constructor<?>> constructors )
    {
        if( clazz != null && !clazz.equals( Object.class ) )
        {
            constructors.addAll( asList( clazz.getDeclaredConstructors() ) );
            addConstructors( clazz.getSuperclass(), constructors );
        }
    }

    public static List<Method> getMethodsOf( Class<?> clazz )
    {
        List<Method> methods = new ArrayList<Method>();
        addMethods( clazz, methods );
        return methods;
    }

    private static void addMethods( Class<?> clazz, List<Method> methods )
    {
        if( clazz != null && !clazz.equals( Object.class ) )
        {
            methods.addAll( asList( clazz.getDeclaredMethods() ) );
            addMethods( clazz.getSuperclass(), methods );
        }
    }

    public static List<Field> getFieldsOf( Class<?> clazz )
    {
        List<Field> fields = new ArrayList<Field>();
        addFields( clazz, fields );
        return fields;
    }

    private static void addFields( Class<?> clazz, List<Field> fields)
    {
        if( clazz != null && !clazz.equals( Object.class ) )
        {
            fields.addAll( asList( clazz.getDeclaredFields() ) );
            addFields( clazz.getSuperclass(), fields );
        }
    }

    private static void addInterfaces( Type type, Set<Class<?>> interfaces )
    {
        if( !interfaces.contains( type ) )
        {
            if( type instanceof ParameterizedType )
            {
                final ParameterizedType parameterizedType = (ParameterizedType) type;
                addInterfaces( parameterizedType.getRawType(), interfaces );
            }
            else if( type instanceof Class )
            {
                Class<?> clazz = (Class<?>) type;

                if( clazz.isInterface() )
                {
                    interfaces.add( clazz );
                }

                Type[] subTypes = clazz.getGenericInterfaces();
                for( Type subType : subTypes )
                {
                    addInterfaces( subType, interfaces );
                }
            }
        }
    }

    private static void addGenericInterfaces( Type type, Set<Type> interfaces )
    {
        if( !interfaces.contains( type ) )
        {
            if( type instanceof ParameterizedType )
            {
                interfaces.add( type );
            }
            else if( type instanceof Class )
            {
                Class<?> clazz = (Class<?>) type;

                if( clazz.isInterface() )
                {
                    interfaces.add( clazz );
                }

                Type[] subTypes = clazz.getGenericInterfaces();
                for( Type subType : subTypes )
                {
                    addGenericInterfaces( subType, interfaces );
                }
            }
        }
    }

    /**
     * Given a type variable, find what it resolves to given the declaring class where type
     * variable was found and a top class that extends the declaring class.
     *
     * @param name
     * @param declaringClass
     * @param topClass
     *
     * @return type
     */
    public static Type resolveTypeVariable( TypeVariable<?> name, Class<?> declaringClass, Class<?> topClass )
    {
        return resolveTypeVariable( name, declaringClass, new HashMap<TypeVariable<?>, Type>(), topClass );
    }

    private static Type resolveTypeVariable( TypeVariable<?> name,
                                             Class<?> declaringClass,
                                             Map<TypeVariable<?>, Type> mappings,
                                             Class<?> current
    )
    {
        if( current.equals( declaringClass ) )
        {
            Type resolvedType = name;
            while( resolvedType instanceof TypeVariable )
            {
                resolvedType = mappings.get( resolvedType );
            }
            return resolvedType;
        }

        for( Type type : current.getGenericInterfaces() )
        {
            Class<?> subClass;
            if( type instanceof ParameterizedType )
            {
                ParameterizedType pt = (ParameterizedType) type;
                Type[] args = pt.getActualTypeArguments();
                Class<?> clazz = (Class<?>) pt.getRawType();
                TypeVariable<?>[] vars = clazz.getTypeParameters();
                for( int i = 0; i < vars.length; i++ )
                {
                    TypeVariable<?> var = vars[ i ];
                    Type mappedType = args[ i ];
                    mappings.put( var, mappedType );
                }
                subClass = (Class<?>) pt.getRawType();
            }
            else
            {
                subClass = (Class<?>) type;
            }

            Type resolvedType = resolveTypeVariable( name, declaringClass, mappings, subClass );
            if( resolvedType != null )
            {
                return resolvedType;
            }
        }

        return Object.class;
    }

    /**
     * Get URI for a class.
     *
     * @param clazz class
     *
     * @return URI
     *
     * @throws NullPointerException if clazz is null
     */
    public static String toURI( final Class<?> clazz )
        throws NullPointerException
    {
        return toURI( clazz.getName() );
    }

    /**
     * Get URI for a class name.
     *
     * Example:
     * Class name com.example.Foo$Bar
     * is converted to
     * URI urn:qi4j:com.example.Foo-Bar
     *
     * @param className class name
     *
     * @return URI
     *
     * @throws NullPointerException if className is null
     */
    public static String toURI( String className )
        throws NullPointerException
    {
        className = normalizeClassToURI( className );
        return "urn:qi4j:type:" + className;
    }

    /**
     * Get class name from a URI
     *
     * @param uri URI
     *
     * @return class name
     *
     * @throws NullPointerException if uri is null
     */
    public static String toClassName( String uri )
        throws NullPointerException
    {
        uri = uri.substring( "urn:qi4j:type:".length() );
        uri = denormalizeURIToClass( uri );
        return uri;
    }

    public static String normalizeClassToURI( String className )
    {
        return className.replace( '$', '-' );
    }

    public static String denormalizeURIToClass( String uriPart )
    {
        return uriPart.replace( '-', '$' );
    }

	public static Object getFieldValue(Object object, String fieldName) {
		if(object==null){
			return null;
		}
		Field field = getFieldOf(object.getClass(), fieldName);
		if(field==null){
			return null;
		}
		try {
			field.setAccessible(true);
			return field.get(object);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}



	//目前计算的不精确，但足够使用
	public static Class<?> getGenericParameterType(Class<?> implClass,
			Class<?> superClass,int positon) {
		for (Class<?> chain : Classes.getClassesOf(implClass)) {
    		Type parent = chain.getGenericSuperclass();
    		if(parent  instanceof ParameterizedType){
    			ParameterizedType tt = (ParameterizedType)parent;
    			Type[] typeArguments=tt.getActualTypeArguments();
    			Preconditions.checkArgument(typeArguments.length>0,"%s - %s",implClass,superClass);
				if(typeArguments[positon] instanceof Class){
					return (Class<?>) typeArguments[positon];
				}
    		}
		}
		return null;
	}

	
	@SafeVarargs
	public static List<Field> getFieldsOf(Class<?> clazz,
			  Predicate<Field> ... predicates) {
	      List<Field> fields = new ArrayList<Field>();
	      Predicate<Field>  p = Predicates.and(predicates);
	      addFields( clazz, fields ,p);
	      return fields;
    }


	/**使用缺省构造器反射创建对象*/
	@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<T> clazz){
		try {
			Constructor<?>[] constructors = clazz.getDeclaredConstructors();
			for (Constructor<?> constructor : constructors) {
				constructor.setAccessible(true);
				if(constructor.getParameterTypes().length==0){
					return (T) constructor.newInstance();
				}
			}
			
			Preconditions.checkArgument(false,"创建[%s]对象失败,需要无参的缺省构造器",clazz);
			return null;//不会运行到此
			
		} catch (Exception e) {
			throw new RuntimeException(String.format("创建[%s]对象失败,需要无参的缺省构造器", clazz),e);
		}
	}
	@SuppressWarnings("unchecked")
	public static <T> T newInstance(String clazz){
		try {
			return (T) newInstance(Class.forName(clazz));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(String.format("创建[%s]对象失败,找不到此类", clazz),e);
		}
	}

    private static void addFields( Class<?> clazz, List<Field> fields ,  Predicate<Field> predicate )
    {
        if( clazz != null && !clazz.equals( Object.class ) )
        {
         	Collection<Field> c =  Collections2.filter(asList( clazz.getDeclaredFields() ), predicate);
            fields.addAll(c);
            addFields( clazz.getSuperclass(), fields ,predicate);
        }
    }

	public static <A extends Annotation> A getAnnotationOf(Class<?> type ,Class<A> annotationClass) {
		Set<Class<?>> classesTree = getSuperClassesOf(type);
		for (Class<?> clazz : classesTree) {
			A result = clazz.getAnnotation(annotationClass);
			if(result !=null){
				return result;
			}
		}
		return null;
	}
	
	public static void setFieldValue(Object object, Field field, Object value) {
		Preconditions.checkNotNull(object,"object 参数应非空");
		Preconditions.checkNotNull(object,"field 参数应非空");

		try {
			field.setAccessible(true);
			field.set(object, value);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 *   从root对象开始用path为字段导航路径来设置字段值
	 *  @path 类似文件路径的对象字段引用路径: "/a/b"
	 */
	public static void setFieldValue(Object root,String path, Object value)  {
 		Iterator<String> segments = path2segments(path).iterator();
		Object current = root;
		Field field=null;
		while(segments.hasNext()){
			String segment = segments.next();
			field = Classes.getFieldOf(current.getClass(), segment);
			if(field==null){
				return;
			}
            if(segments.hasNext()){
     			Object fieldValue = Classes.getFieldValue(current, field);
    			if(fieldValue==null){
    				fieldValue = Classes.newInstance(field.getType());
    				Classes.setFieldValue(current, field,fieldValue);
    			}
    			current = fieldValue;
            }
		}
		if(current!=null&&field!=null){
			Classes.setFieldValue(current, field, value);
		}
	}
	/**
	 *   从root对象开始用path为字段导航路径来获取字段值
	 *   @path 类似文件路径的对象字段引用路径: "/a/b"
	 */
	public static Object getFieldValueByPath(Object root,String path) {
		Iterable<String> segments = path2segments(path);
		Object current = root;
		for (String segment : segments) {
 			current = Classes.getFieldValue(current, segment);
			if(current==null){
				break;
			}
		}
		return current;
	}

	private static Iterable<String> path2segments(String path) {
		return Splitter.on("/").trimResults().omitEmptyStrings().split(path);
	}
	
	public static Object getFieldValue(Object object, Field field) {
		Preconditions.checkNotNull(object,"object参数应非空");
		Preconditions.checkNotNull(field,"field参数应非空");

		try {
			field.setAccessible(true);
			return field.get(object);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	public static Object safeGetFieldValue(Object object, Field field) {
		if(object==null||field==null){
			return null;
		}
		try {
			return getFieldValue(object,field);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static Field safeGetFieldOf(Class<?> clazz, String name) {
		if(clazz==null||name==null){
			return null;
		}
		try {
			return getFieldOf(clazz,name);
		} catch (Exception e) {
			return null;
		}
	}
	public static Field getFieldOf(Class<?> clazz, String name) {
		Preconditions.checkNotNull(name,"name参数应非空");
        if( clazz != null ){
        	try {
				return clazz.getDeclaredField(name);
			} catch (SecurityException e) {
				throw new RuntimeException(e);
			} catch (NoSuchFieldException e) {
				return getFieldOf(clazz.getSuperclass(),name);
			}
        }
		return null;
	}
}
