package it.wverdese.rxgithub;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

/**
 * Created by walter on 23/03/15.
 */
public class RxGitHubActivity extends Activity
{
    private static final String API_CALL = "https://api.github.com/users?since=%s";

    private Toolbar           mCardToolbar;
    private ListView          mListView;
    private GitHubListAdapter mAdapter;
    private ProgressBar       mProgress;

    private Subscription subscription;

    Observable<Void> clicks;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rxgithub);

        mCardToolbar = (Toolbar)findViewById(R.id.card_toolbar);
        mProgress = (ProgressBar)findViewById(R.id.toolbar_progress);
        mListView = (ListView)findViewById(R.id.card_list);
        mCardToolbar.inflateMenu(R.menu.menu_card);

        clicks = Observable
                .create((Subscriber<? super Void> subscriber) -> {
                    mCardToolbar.setOnMenuItemClickListener(menuItem -> {
                        switch (menuItem.getItemId())
                        {
                            case R.id.action_refresh:
                                subscriber.onNext(null);
                                return true;
                        }
                        return false;
                    });
                    subscriber.add(Subscriptions.create(() -> mCardToolbar.setOnMenuItemClickListener(null)));
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .share();

        createSubscription();
    }

    private void createSubscription()
    {
        if (clicks == null || isFinishing()) return;
        unsubscribe();
        subscription = clicks
                .startWith((Void)null)
                .map($ -> String.format(API_CALL, (int)Math.floor(Math.random() * 500)))
                .switchMap(s -> Observable
                        .create((Subscriber<? super User[]> subscriber) -> {
                            try
                            {
                                // signal request start
                                subscriber.onNext(null);

                                //do http request
                                URL url = new URL(s);
                                HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
                                InputStream in = urlConnection.getInputStream();

                                //parse JSON
                                Gson gson = new Gson();
                                User[] data = gson.fromJson(new InputStreamReader(in), User[].class);

                                subscriber.onNext(data);
                                subscriber.onCompleted();
                            }
                            catch (Throwable e)
                            {
                                subscriber.onError(e);
                            }
                        })
                        .subscribeOn(Schedulers.io()))
                .observeOn(AndroidSchedulers.mainThread())
                .retry((count, error) -> {
                    showProgress(false);
                    error.printStackTrace();
                    Toast.makeText(RxGitHubActivity.this, "An error occurred", Toast.LENGTH_LONG).show();
                    //XXX if error is recoverable, return true to retry (== resubscribe to the previous observable)
                    return false;
                })
                .subscribe(users -> {
                    if (users == null) showProgress(true);
                    else
                    {
                        showProgress(false);
                        if (mAdapter == null)
                        {
                            mAdapter = new GitHubListAdapter(users);
                            mListView.setAdapter(mAdapter);
                        }
                        else
                        {
                            mAdapter.replaceData(users);
                        }
                    }
                }, error -> {
                    // at this point the chain will unsubscribe, let's hide the refresh button
                    mCardToolbar.getMenu().findItem(R.id.action_refresh).setVisible(false);
                });
    }

    private void unsubscribe()
    {
        if (subscription != null)
        {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    @Override
    protected void onDestroy()
    {
        unsubscribe();
        super.onDestroy();
    }

    //handle the toolbar's items visibility
    private void showProgress(boolean show)
    {
        mProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        mCardToolbar.getMenu().findItem(R.id.action_refresh).setVisible(!show);
    }

    private void toggleProgress()
    {
        mProgress.setVisibility(mProgress.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }

    //the Adapter for the ListView
    class GitHubListAdapter extends BaseAdapter
    {
        private List<User> users;

        GitHubListAdapter(User[] u)
        {
            init(u);
        }

        private void init(User[] u)
        {
            users = new ArrayList<>();
            if (u != null)
                for (int i = 0; i < 3; i++)
                    users.add(u[i]);
        }

        public void replaceData(User[] u)
        {
            init(u);
            notifyDataSetChanged();
        }

        @Override
        public int getCount()
        {
            return users.size();
        }

        @Override
        public User getItem(int position)
        {
            return users.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return getItem(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            SuggestionWrapper wrapper;
            if (convertView == null)
            {
                convertView = getLayoutInflater().inflate(R.layout.suggestion_list_row, parent, false);
                wrapper = new SuggestionWrapper(convertView);
                convertView.setTag(wrapper);
            }
            else
            {
                wrapper = (SuggestionWrapper)convertView.getTag();
            }

            User item = getItem(position);

            wrapper.getTitle().setText(item.getName());

            return convertView;
        }
    }

    class SuggestionWrapper
    {
        private View base;

        private TextView  mText;
        private ImageView mImageView;

        SuggestionWrapper(View base)
        {
            this.base = base;
        }

        public TextView getTitle()
        {
            if (mText == null)
                mText = (TextView)base.findViewById(R.id.suggestion_name);
            return mText;
        }

        public ImageView getPhoto()
        {
            if (mImageView == null)
                mImageView = (ImageView)base.findViewById(R.id.suggestion_photo);
            return mImageView;
        }
    }
}