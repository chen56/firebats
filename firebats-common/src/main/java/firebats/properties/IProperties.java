package firebats.properties;
/**
 * 表达和java.util.Properties类似的键值属性配置概念
 */
public interface IProperties {
    String get(String key);
    void put(String key,String value);
}