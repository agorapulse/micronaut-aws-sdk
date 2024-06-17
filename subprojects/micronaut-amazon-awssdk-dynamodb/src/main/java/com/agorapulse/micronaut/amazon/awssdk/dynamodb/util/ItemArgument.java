package com.agorapulse.micronaut.amazon.awssdk.dynamodb.util;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.type.Argument;
import org.reactivestreams.Publisher;

import java.util.Optional;

public class ItemArgument {

    public static <T> Optional<ItemArgument> findItemArgument(Class<T> itemType, MethodInvocationContext<Object, Object> context) {
        Argument<?>[] args = context.getArguments();

        if (args.length == 1) {
            Argument<?> itemArgument = args[0];
            if (itemArgument.getType().isArray() || Iterable.class.isAssignableFrom(itemArgument.getType()) || Publisher.class.isAssignableFrom(itemArgument.getType())) {
                ItemArgument item = new ItemArgument();
                item.argument = itemArgument;
                item.single = false;
                return Optional.of(item);
            }

            if (itemType.isAssignableFrom(itemArgument.getType())) {
                ItemArgument item = new ItemArgument();
                item.argument = itemArgument;
                item.single = true;
                return Optional.of(item);
            }
        }

        return Optional.empty();
    }


    Argument<?> argument;
    boolean single;

    public Argument<?> getArgument() {
        return argument;
    }

    public boolean isSingle() {
        return single;
    }
}
