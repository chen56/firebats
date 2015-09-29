package firebats.internal.bus.rx;

import java.util.concurrent.CopyOnWriteArrayList;

import rx.Observer;
/**PublishSubject作用同自制的CompositeObserver，但PublishSubject要慢5%~10%
 * 
 * 	public synchronized Observable<IRawRequest> receive() {
		//return receives;//PublishSubject
 		return Observable.create(new OnSubscribe<IRawRequest>() {
			@Override
			public void call(final Subscriber<? super IRawRequest> s) {
				final Observer<IRawRequest> observer=new Observer<IRawRequest>() {
					@Override
					public void onNext(IRawRequest t) {
						s.onNext(t);
					}
					@Override
					public void onError(Throwable e) {
						s.onError(e);
					}
					@Override
					public void onCompleted() {
						s.onCompleted();
					}
				};
				receives.add(observer);
				s.add(Subscriptions.create(new Action0(){
					@Override
					public void call() {
						receives.underlyingObservers.remove(observer);
					}}));
			}
		});
	}
 * */
public class CompositeObserver<T> implements Observer<T> {
    private final CopyOnWriteArrayList<Observer<T>> underlyingObservers=new CopyOnWriteArrayList<>();
    protected void add(Observer<T> underlyingObserver) {
        this.underlyingObservers.add(underlyingObserver);
    }
    @Override
    public void onCompleted() {
        for (Observer<T> underlyingObserver : underlyingObservers) {
            underlyingObserver.onCompleted();
        }
    }

    @Override
    public void onError(Throwable e) {
        for (Observer<T> underlyingObserver : underlyingObservers) {
            underlyingObserver.onError(e);
        }
    }
    @Override
    public void onNext(T t) {
        for (Observer<T> underlyingObserver : underlyingObservers) {
            underlyingObserver.onNext(t);
        }
    }
}