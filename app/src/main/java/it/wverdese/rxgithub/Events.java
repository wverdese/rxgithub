package it.wverdese.rxgithub;

import android.view.View;

import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by walter on 23/03/15.
 */
public class Events {

    /*
     * Creates a subject that emits events for each click on view
     */
    public static Observable<View> click(View view) {
        Observable.create((Subscriber<? super View> subscriber) -> {
            view.setOnClickListener(subscriber::onNext);
            // the following adds an "unsubscribe callback"
            subscriber.add(Subscriptions.create(() -> view.setOnClickListener(null)));
        });
    }

}
