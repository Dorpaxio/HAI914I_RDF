package qengine.program.Models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Index {

    public enum Order {
        SPO(new int[] {0, 1, 2}),
        SOP(new int[] {0, 2, 1}),
        PSO(new int[] {1, 0, 2}),
        POS(new int[] {1, 2, 0}),
        OSP(new int[] {2, 0, 1}),
        OPS(new int[] {2, 1, 0});

        private final int[] order;
        Order(int[] order) {
            this.order = order;
        }

        public int[] getOrder() {
            return order;
        }
    }

    public static class Store extends HashMap<Integer, HashMap<Integer, Set<Integer>>> {
        public void put(int a, int b, int c) {
            HashMap<Integer, Set<Integer>> action = computeIfAbsent(a, k -> new HashMap<>());
            Set<Integer> objects = action.computeIfAbsent(b, k -> new HashSet<>());
            objects.add(c);
        }
    }

    private final HashMap<Order, Store> stores = new HashMap<>();

    public Index() {
        Arrays.stream(Order.values()).forEach(order -> stores.put(order, new Store()));
    }

    public void put(int subject, int predicate, int object) {
        final int[] fields = {subject, predicate, object};
        stores.forEach((order, store) -> {
            final int[] orderIds = order.getOrder();
            store.put(fields[orderIds[0]], fields[orderIds[1]], fields[orderIds[2]]);
        });
    }

    @Override
    public String toString() {
        return "{" + stores + "}";
    }
}
