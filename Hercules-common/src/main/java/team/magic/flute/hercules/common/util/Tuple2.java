package team.magic.flute.hercules.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A generic tuple class that holds two related values.
 *
 * <p>This immutable data structure is useful for returning two values from a method
 * or grouping two related objects together. It provides a simple key-value pair
 * implementation with type safety through generics.
 *
 * <p>Common use cases include:
 * <ul>
 *   <li>Returning multiple values from a method</li>
 *   <li>Representing key-value pairs in collections</li>
 *   <li>Grouping related data temporarily</li>
 *   <li>Functional programming patterns</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * Tuple2<String, Integer> nameAge = new Tuple2<>("John", 25);
 * String name = nameAge.getKey();
 * Integer age = nameAge.getValue();
 *
 * // Using in collections
 * List<Tuple2<String, Double>> coordinates = Arrays.asList(
 *     new Tuple2<>("x", 10.5),
 *     new Tuple2<>("y", 20.3)
 * );
 * }</pre>
 *
 * @param <K> the type of the key (first element)
 * @param <V> the type of the value (second element)
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain=true)
public class Tuple2 <K,V>{

    /**
     * The key (first element) of the tuple.
     * This field is immutable once set during construction.
     */
    private K key;

    /**
     * The value (second element) of the tuple.
     * This field is immutable once set during construction.
     */
    private V value;
}
