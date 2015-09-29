package firebats.internal.bus.rx;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Action1;

public final class OperatorSuppressError<T> implements Operator<T, T> {
	final Action1<Throwable> onError;
	final static Action1<Throwable> DefaultOnError=new Action1<Throwable>(){
		@Override
		public void call(Throwable t1) {
		}};

	public OperatorSuppressError(Action1<Throwable> onError) {
		this.onError = onError;
	}

	public static <T> OperatorSuppressError<T> onError(Action1<Throwable> onError) {
			return new OperatorSuppressError<T>(onError);
	}
	public static <T> OperatorSuppressError<T> onDummy() {
			return new OperatorSuppressError<T>(DefaultOnError);
	}

	@Override
	public Subscriber<? super T> call(final Subscriber<? super T> t1) {
		return new Subscriber<T>(t1) {
			@Override
			public void onNext(T t) {
				t1.onNext(t);
			}

			@Override
			public void onError(Throwable e) {
				onError.call(e);
			}

			@Override
			public void onCompleted() {
				t1.onCompleted();
			}

		};
	}
}
