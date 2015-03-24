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
    public static Observable<Object> click(View view) {
        final PublishSubject<Object> subject = PublishSubject.create();
        view.setOnClickListener(v -> subject.onNext(new Object()));
        return subject;
    }

}
