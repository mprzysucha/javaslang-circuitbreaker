/*
 *
 *  Copyright 2015 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package javaslang.circuitbreaker;

import javaslang.Function2;
import javaslang.Function3;
import javaslang.Function4;
import javaslang.Function5;
import javaslang.Function6;
import javaslang.Function7;
import javaslang.Function8;
import javaslang.control.Try;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A CircuitBreaker manages the state of a backend system. It is notified on the result of all
 * attempts to communicate with the backend, via the {@link #recordSuccess} and {@link #recordFailure} methods.
 * Before communicating with the backend, the respective connector must obtain the permission to do so via the method
 * {@link #isCallPermitted()}.
 */
public interface CircuitBreaker {

    /**
     * Requests permission to call this circuitBreaker's backend.
     *
     * @return boolean whether a call should be permitted
     */
    boolean isCallPermitted();

    /**
     * Records a failed call.
     * This method must be invoked after a failed call.
     *
     * @param throwable The throwable which must be recorded
     */
    void recordFailure(Throwable throwable);

     /**
      * Records a successful call.
      * This method must be invoked after a successful call.
      */
    void recordSuccess();

    /**
     * Get the name of this CircuitBreaker
     *
     * @return the name of this CircuitBreaker
     */
    String getName();

    /**
     * Get the state of this CircuitBreaker
     *
     * @return the state of this CircuitBreaker
     */
    State getState();

    /**
     * Get the CircuitBreakerConfig of this CircuitBreaker.
     *
     * @return the CircuitBreakerConfig of this CircuitBreaker
     */
    CircuitBreakerConfig getCircuitBreakerConfig();

    /**
     * Get the Metrics of this CircuitBreaker.
     *
     * @return the Metrics of this CircuitBreaker
     */
    Metrics getMetrics();

    /**
     * States of the CircuitBreaker state machine.
     */
    enum State {
        /** A CLOSED breaker is operating normally and allowing
         requests through. */
        CLOSED,
        /** An OPEN breaker has tripped and will not allow requests
         through. */
        OPEN,
        /** A HALF_OPEN breaker has completed its wait interval
         and will allow requests */
        HALF_OPEN
    }

    /**
     * State transitions of the CircuitBreaker state machine.
     */
    enum StateTransition {
        CLOSED_TO_OPEN(State.CLOSED, State.OPEN),
        HALF_OPEN_TO_CLOSED(State.HALF_OPEN, State.CLOSED),
        HALF_OPEN_TO_OPEN(State.HALF_OPEN, State.OPEN),
        OPEN_TO_HALF_OPEN(State.OPEN, State.HALF_OPEN);

        State fromState;
        State toState;

        StateTransition(State fromState, State toState) {
            this.fromState = fromState;
            this.toState = toState;
        }

        public State getFromState() {
            return fromState;
        }

        public State getToState() {
            return toState;
        }

        @Override
        public String toString(){
            return String.format("State transition from %s to %s", fromState, toState);
        }
    }

    interface Metrics {

        /**
         * Returns the failure rate in percentage. If the number of measured calls is below the minimum number of measured calls,
         * it returns -1.
         *
         * @return the failure rate in percentage
         */
        float getFailureRate();

        /**
         * Returns the current number of buffered calls.
         *
         * @return he current number of buffered calls
         */
        int getNumberOfBufferedCalls();

        /**
         * Returns the current number of failed calls.
         *
         * @return the current number of failed calls.
         */
        int getNumberOfFailedCalls();
    }

    /**
     * Creates a supplier which is secured by a CircuitBreaker.
     *
     * @param supplier the original supplier
     * @param circuitBreaker the CircuitBreaker
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(Try.CheckedSupplier<T> supplier, CircuitBreaker circuitBreaker){
        return () -> withReturnChecked(() -> supplier.get(), circuitBreaker);
    }

    /**
     * Creates a runnable which is secured by a CircuitBreaker.
     *
     * @param runnable the original runnable
     * @param circuitBreaker the CircuitBreaker
     * @return a runnable which is secured by a CircuitBreaker.
     */
    static Try.CheckedRunnable decorateCheckedRunnable(Try.CheckedRunnable runnable, CircuitBreaker circuitBreaker){
        return () -> withoutReturnChecked(() -> runnable.run(), circuitBreaker);
    }

    /**
     * Creates a supplier which is secured by a CircuitBreaker.
     *
     * @param supplier the original supplier
     * @param circuitBreaker the CircuitBreaker
     * @return a supplier which is secured by a CircuitBreaker.
     */
    static <T> Supplier<T> decorateSupplier(Supplier<T> supplier, CircuitBreaker circuitBreaker){
        return () -> withReturn(() -> supplier.get(), circuitBreaker);
    }

    /**
     * Creates a consumer which is secured by a CircuitBreaker.
     *
     * @param consumer the original consumer
     * @param circuitBreaker the CircuitBreaker
     * @return a consumer which is secured by a CircuitBreaker.
     */
    static <T> Consumer<T> decorateConsumer(Consumer<T> consumer, CircuitBreaker circuitBreaker){
        return (t) -> withoutReturn(() -> consumer.accept(t), circuitBreaker);
    }

    /**
     * Creates a runnable which is secured by a CircuitBreaker.
     *
     * @param runnable the original runnable
     * @param circuitBreaker the CircuitBreaker
     * @return a runnable which is secured by a CircuitBreaker.
     */
    static Runnable decorateRunnable(Runnable runnable, CircuitBreaker circuitBreaker){
        return () -> withoutReturn(() -> runnable.run(), circuitBreaker);
    }

    /**
     * Creates a function which is secured by a CircuitBreaker.
     *
     * @param function the original function
     * @param circuitBreaker the CircuitBreaker
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T, R> Function<T, R> decorateFunction(Function<T, R> function, CircuitBreaker circuitBreaker){
        return (T t) -> withReturn(() -> function.apply(t), circuitBreaker);
    }

    /**
     * Creates a function  with two arguments which is secured by a CircuitBreaker.
     *
     * @param function2 the original function
     * @param circuitBreaker the CircuitBreaker
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T1, T2, R> Function2<T1, T2, R> decorateFunction2(Function2<T1, T2, R> function2, CircuitBreaker circuitBreaker){
        return (T1 t1, T2 t2) -> withReturn(() -> function2.apply(t1, t2), circuitBreaker);
    }

    /**
     * Creates a function  with three arguments which is secured by a CircuitBreaker.
     *
     * @param function3 the original function
     * @param circuitBreaker the CircuitBreaker
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T1, T2, T3, R> Function3<T1, T2, T3, R> decorateFunction3(Function3<T1, T2, T3, R> function3, CircuitBreaker circuitBreaker){
        return (T1 t1, T2 t2, T3 t3) -> withReturn(() -> function3.apply(t1, t2, t3), circuitBreaker);
    }

    /**
     * Creates a function  with four arguments which is secured by a CircuitBreaker.
     *
     * @param function4 the original function
     * @param circuitBreaker the CircuitBreaker
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T1, T2, T3, T4, R> Function4<T1, T2, T3, T4, R> decorateFunction4(Function4<T1, T2, T3, T4, R> function4, CircuitBreaker circuitBreaker){
        return (T1 t1, T2 t2, T3 t3, T4 t4) -> withReturn(() -> function4.apply(t1, t2, t3, t4), circuitBreaker);
    }

    /**
     * Creates a function  with five arguments which is secured by a CircuitBreaker.
     *
     * @param function5 the original function
     * @param circuitBreaker the CircuitBreaker
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T1, T2, T3, T4, T5, R> Function5<T1, T2, T3, T4, T5, R> decorateFunction5(Function5<T1, T2, T3, T4, T5, R> function5, CircuitBreaker circuitBreaker){
        return (T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) -> withReturn(() -> function5.apply(t1, t2, t3, t4, t5), circuitBreaker);
    }

    /**
     * Creates a function  with six arguments which is secured by a CircuitBreaker.
     *
     * @param function6 the original function
     * @param circuitBreaker the CircuitBreaker
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T1, T2, T3, T4, T5, T6, R> Function6<T1, T2, T3, T4, T5, T6, R> decorateFunction6(Function6<T1, T2, T3, T4, T5, T6, R> function6, CircuitBreaker circuitBreaker){
        return (T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) -> withReturn(() -> function6.apply(t1, t2, t3, t4, t5, t6), circuitBreaker);
    }

    /**
     * Creates a function  with seven arguments which is secured by a CircuitBreaker.
     *
     * @param function7 the original function
     * @param circuitBreaker the CircuitBreaker
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T1, T2, T3, T4, T5, T6, T7, R> Function7<T1, T2, T3, T4, T5, T6, T7, R> decorateFunction7(Function7<T1, T2, T3, T4, T5, T6, T7, R> function7, CircuitBreaker circuitBreaker){
        return (T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) -> withReturn(() -> function7.apply(t1, t2, t3, t4, t5, t6, t7), circuitBreaker);
    }

    /**
     * Creates a function  with eight arguments which is secured by a CircuitBreaker.
     *
     * @param function8 the original function
     * @param circuitBreaker the CircuitBreaker
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> decorateFunction8(Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> function8, CircuitBreaker circuitBreaker){
        return (T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) -> withReturn(() -> function8.apply(t1, t2, t3, t4, t5, t6, t7, t8), circuitBreaker);
    }

    /**
     * Creates a function which is secured by a CircuitBreaker.
     *
     * @param function the original function
     * @param circuitBreaker the CircuitBreaker
     * @return a function which is secured by a CircuitBreaker.
     */
    static <T, R> Try.CheckedFunction<T, R> decorateCheckedFunction(Try.CheckedFunction<T, R> function, CircuitBreaker circuitBreaker){
        return (T t) -> withReturnChecked(() -> function.apply(t), circuitBreaker);
    }

    static <R> R withReturn(Supplier<R> supplier, CircuitBreaker circuitBreaker) {
        try {
            return withReturnChecked(() -> supplier.get(), circuitBreaker);
        } catch (Throwable throwable) {
            throw (RuntimeException) throwable;
        }
    }

    static void withoutReturn(Runnable runnable, CircuitBreaker circuitBreaker) {
        try {
            withoutReturnChecked(() -> runnable.run(), circuitBreaker);
        } catch (Throwable throwable) {
            throw (RuntimeException) throwable;
        }
    }


    static <R> R withReturnChecked(Try.CheckedSupplier<R> supplier, CircuitBreaker circuitBreaker) throws Throwable {
        CircuitBreakerUtils.isCallPermitted(circuitBreaker);
        try{
            R returnValue = supplier.get();
            circuitBreaker.recordSuccess();
            return returnValue;
        } catch (Throwable throwable){
            circuitBreaker.recordFailure(throwable);
            throw throwable;
        }
    }


    static void withoutReturnChecked(Try.CheckedRunnable runnable, CircuitBreaker circuitBreaker) throws Throwable {
        CircuitBreakerUtils.isCallPermitted(circuitBreaker);
        try{
            runnable.run();
            circuitBreaker.recordSuccess();
        } catch (Throwable throwable){
            circuitBreaker.recordFailure(throwable);
            throw throwable;
        }
    }

}
