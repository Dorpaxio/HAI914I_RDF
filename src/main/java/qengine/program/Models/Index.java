package qengine.program.Models;

import java.util.*;

public class Index {

    public enum Order {
        SPO(new int[]{0, 1, 2}),
        SOP(new int[]{0, 2, 1}),
        PSO(new int[]{1, 0, 2}),
        POS(new int[]{1, 2, 0}),
        OSP(new int[]{2, 0, 1}),
        OPS(new int[]{2, 1, 0});

        private final int[] order;

        Order(int[] order) {
            this.order = order;
        }

        public int[] getOrder() {
            return order;
        }

        public static Order getBestOrder(int subject, int predicate, int object) {
            subject = subject > 0 ? 1 : 0;
            predicate = predicate > 0 ? 1 : 0;
            object = object > 0 ? 1 : 0;

            if (subject == 1 && predicate == 1 && object == 0) return SPO;
            else if (subject == 1 && predicate == 0 && object == 1) return SOP;
            else return POS;
        }
    }

    public static class Store extends HashMap<Integer, HashMap<Integer, Set<Integer>>> {
        public void put(int a, int b, int c) {
            Map<Integer, Set<Integer>> action = computeIfAbsent(a, k -> new HashMap<>());
            Set<Integer> objects = action.computeIfAbsent(b, k -> new TreeSet<>());
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

    public Set<Integer> getResults(int subject, int predicate, int object, Order order) {
        final int[] fields = {subject, predicate, object};
        final int[] orderIds = order.getOrder();
        return Optional.ofNullable(stores.get(order).get(fields[orderIds[0]]).get(fields[orderIds[1]]))
                .orElse(new HashSet<>());
    }

    @Override
    public String toString() {
        return "{" + stores + "}";
    }
}
