package glass.chungwon.verse;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ItemListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    /**
     * An array of verse items. It's set to static as it is needed to restore session.
     */
    public static ArrayList<VerseItem> ITEMS = new ArrayList<>();

    /**
     * A logging tag for Logcat.
     */
    private final String TAG = "ItemListActivity";

    /**
     * A map of verse items, by ID.
     */
    public static Map<String, VerseItem> ITEM_MAP = new HashMap<>();

    private SimpleItemRecyclerViewAdapter recyclerViewAdapter;
    private RecyclerView recyclerView;
    private AppCompatImageButton querySubmitButton;
    private AppCompatEditText editText;
    LinearLayoutManager mLayoutManager;
    public static int index = -1;
    public static int top = -1;
    private static String queryString;

    Parcelable state;
    TextView hyperLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        hyperLink = (TextView)findViewById(R.id.privacyLinkView);
        editText = (AppCompatEditText) findViewById(R.id.search_text);
        querySubmitButton = (AppCompatImageButton) findViewById(R.id.submit_query_button);
        recyclerView = findViewById(R.id.item_list);
        assert recyclerView != null;
        mLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            Log.d(TAG,"recreating a previously destroyed instance");
            // Restore value of members from saved state
            editText.setText(savedInstanceState.getString("queryString"));
            performSearch();
            mLayoutManager.onRestoreInstanceState(state);
        }  // Else { Probably initialize members with default values for a new instance }


        editText.setImeActionLabel("Search", KeyEvent.KEYCODE_ENTER);
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (event!=null && (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN))) {
                performSearch();

                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

                if(imm.isAcceptingText() && getCurrentFocus() != null) { // verify if the soft keyboard is open
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        querySubmitButton.setOnClickListener(v -> {
            performSearch();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(querySubmitButton.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        });


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            // TODO: Not implemented, will open new Chrome page with the url of the verse, currently invisible in the UI
            Snackbar.make(view, "Going to verse URL", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        });

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
        //searchForVerse(searchText);

        recyclerViewAdapter = new SimpleItemRecyclerViewAdapter(this, ITEMS, mTwoPane);
        // set up recycler view
        setupRecyclerView((RecyclerView) recyclerView);
    }

    public void onPrivacyPolicyClick(View v) {

        Intent intent = new Intent(getApplicationContext(),
                PrivacyPolicyActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's query
        editText = (AppCompatEditText) findViewById(R.id.search_text);
        recyclerView = findViewById(R.id.item_list);

        String query = Objects.requireNonNull(editText.getText()).toString();
        Log.d(TAG,"Saved queryString: " + query);
        savedInstanceState.putString("queryString", query);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
    @Override
    public void onPause() {
        super.onPause();

        state = Objects.requireNonNull(recyclerView.getLayoutManager()).onSaveInstanceState();
        //read current recyclerview position
        index = mLayoutManager.findFirstVisibleItemPosition();
        View v = recyclerView.getChildAt(0);
        top = (v == null) ? 0 : (v.getTop() - recyclerView.getPaddingTop());

        editText = (AppCompatEditText) findViewById(R.id.search_text);
        queryString = Objects.requireNonNull(editText.getText()).toString();
    }
    @Override
    public void onResume() {
        super.onResume();

        mLayoutManager.onRestoreInstanceState(state);
        //set recyclerview position
        if(index != -1)
        {
            mLayoutManager.scrollToPositionWithOffset( index, top);
        }
        if(!Objects.equals(queryString, "")) {
            editText = (AppCompatEditText) findViewById(R.id.search_text);
            editText.setText(queryString);
        }
    }
    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG,"restoring saved instance");

        editText = (AppCompatEditText) findViewById(R.id.search_text);
        recyclerView = findViewById(R.id.item_list);

        // Restore value of members from saved state
        editText.setText(savedInstanceState.getString("queryString"));
        performSearch();
        ((LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager())).scrollToPositionWithOffset(savedInstanceState.getInt("lastFirstVisiblePosition"),0);
        savedInstanceState.putInt("lastFirstVisiblePosition", 0);
    }

    private void performSearch() {
        editText = (AppCompatEditText) findViewById(R.id.search_text);
        String query = Objects.requireNonNull(editText.getText()).toString();
        Log.d(TAG, "received search param: " + query);
        String searchText;
        try {
            searchText = URLEncoder.encode(query, "utf-8");
            searchForVerse(searchText);
            editText.clearFocus();
        } catch (UnsupportedEncodingException ex) {
            Log.e(TAG, "utf-8 encoding is unsupported");
        }
    }

    private void searchForVerse(String query) {
        String url = "https://chungwon.glass:8443/query?q=";
        Log.d(TAG, "sending request to: " + url + query);

        // Volley request to the url that populates the recycler view
        JsonArrayRequest getRequest = new JsonArrayRequest(Request.Method.GET, url + query, null,
            response -> {
                Type verseListType = new TypeToken<ArrayList<VerseItem>>(){}.getType();
                Gson gson = new Gson();
                ITEMS = gson.fromJson(String.valueOf(response), verseListType);
                for(VerseItem verse : ITEMS) {
                    ITEM_MAP.put(verse.id, verse);
                }
                recyclerViewAdapter.swap(ITEMS);

            },
            error -> Log.e(TAG, error.toString())
        );

        //Instantiate the RequestQueue and add the request to the queue
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        queue.add(getRequest);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final ItemListActivity mParentActivity;
        private List<VerseItem> mValues;
        private final boolean mTwoPane;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VerseItem item = (VerseItem) view.getTag();
                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putString(ItemDetailFragment.ARG_ITEM_ID, item.id);
                    ItemDetailFragment fragment = new ItemDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.item_detail_container, fragment)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, ItemDetailActivity.class);
                    intent.putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id);

                    context.startActivity(intent);
                }
            }
        };

        @SuppressLint("NotifyDataSetChanged")
        public void swap(List<VerseItem> list){
            if (mValues != null) {
                mValues.clear();
                mValues.addAll(list);
            }
            else {
                mValues = list;
            }
            notifyDataSetChanged();
        }

        SimpleItemRecyclerViewAdapter(ItemListActivity parent,
                                      List<VerseItem> items,
                                      boolean twoPane) {
            mValues = items;
            mParentActivity = parent;
            mTwoPane = twoPane;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_content, parent, false);
            return new ViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mIdView.setText(mValues.get(position).title + "\n" + mValues.get(position).reference);
            holder.mContentView.setText(mValues.get(position).content);

            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mIdView;
            final TextView mContentView;

            ViewHolder(View view) {
                super(view);
                mIdView = (TextView) view.findViewById(R.id.id_text);
                mContentView = (TextView) view.findViewById(R.id.content);
            }
        }
    }

}
