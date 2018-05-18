package com.example.andrea.lab11;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoQuery;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.ObservableSnapshotArray;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class ResultsList extends AppCompatActivity {

    private String deBugTag;
    private RecyclerView.Adapter adapter;
    private RecyclerView list;
    private TextView emptyListMessage;
    private CopyOnWriteArrayList<BookInfo> bookList;
    private List<String> bookIdList;
    private HashSet<String> usersId;
    private Query query;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deBugTag = this.getClass().getName();
        bookList = new CopyOnWriteArrayList<>();

        //set query based on the user research
        Intent intent = getIntent();
        query = FirebaseDatabase.getInstance().getReference().child("books");
        String keyExtra;
        String valueExtra;


        if(intent.getStringExtra("author")!=null){
            keyExtra = "author";
        }else if(intent.getStringExtra("bookTitle")!=null){
            keyExtra = "bookTitle";
        }else if(intent.getStringExtra("ISBN")!=null){
            keyExtra = "ISBN";
        }else if(intent.getStringExtra("publisher")!=null){
            keyExtra = "publisher";
        }else if(intent.getSerializableExtra("usersList")!=null){
            keyExtra = "usersList";
            usersId = (HashSet<String>) intent.getSerializableExtra("usersList");
        }else{
            keyExtra = null;
            bookIdList = (List<String>) intent.getSerializableExtra("bookIdList");
        }

        setContentView(R.layout.recycler_view_search_list);

        //set toolbar
        ImageButton backArrow = findViewById(R.id.backButton);
        backArrow.setOnClickListener((v) -> {onBackPressed();});
        ImageButton mapButton = findViewById(R.id.mapButton);
        if(keyExtra==null){
            mapButton.setVisibility(View.GONE);
        }else if(keyExtra != "usersList"){
            valueExtra = intent.getStringExtra(keyExtra);

            normalResearch(valueExtra);

            mapButton.setOnClickListener((v) -> {
                Intent mapIntent = new Intent(getApplicationContext(),search_results_map.class);
                mapIntent.putExtra(keyExtra,valueExtra);
                startActivity(mapIntent);
                finish();
            });
        } else {
            mapButton.setVisibility(View.GONE);
        }

        //get elements
        list = findViewById(R.id.rv);
        emptyListMessage = findViewById(R.id.emptyListMessage);

        //set adapter
        adapter = new RecyclerView.Adapter <CardViewBook>() {

            @Override
            public CardViewBook onCreateViewHolder(ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_view_search_results_list, parent, false);

                return new CardViewBook(view);
            }

            @Override
            public void onBindViewHolder(@NonNull CardViewBook holder, int position) {

                BookInfo book = new BookInfo(bookList.get(position));
                holder.bindData(book.getBookTitle(),book.getAuthor(),book.get_ISBN(), book.getEditionYear(), book.getBookID(),true);

            }

            @Override
            public int getItemCount() {
                return bookList.size();
            }
        };

        if(keyExtra == null)
            searchFromMapMarker(query);
        else if(keyExtra.equals("usersList"))
            searchFromUsers();


        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    //normal research of book based on author, title, ecc.
    private void normalResearch(String valueExtra){

        if (valueExtra.matches("[0-9]+")) {
            Query query4 = query.orderByChild("ISBN").equalTo(valueExtra);
            //event listener for ISBN query
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                    BookInfo book = parseDataSnapshotBook(dataSnapshot);
                    if(book == null)
                        return;
                    bookList.add(book);
                    adapter.notifyDataSetChanged();
                    emptyListMessage.setVisibility(View.GONE);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                    //todo testare
                    BookInfo book = parseDataSnapshotBook(dataSnapshot);
                    if(book == null)
                        return;
                    bookList.remove(book);
                    if(bookList.size()==0)
                        emptyListMessage.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(deBugTag,databaseError.getMessage()+databaseError.getCode());
                    networkProblem(databaseError);
                }
            };

            query4.addChildEventListener(childEventListener);
        }
        else {
            for(int i=0; i<3; i++){
                if(i==0) {
                    Query query1 = query.orderByChild("author").equalTo(valueExtra);
                    ChildEventListener childEventListener = new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                            BookInfo book = parseDataSnapshotBook(dataSnapshot);
                            if (book == null)
                                return;
                            bookList.add(book);
                            adapter.notifyDataSetChanged();
                            emptyListMessage.setVisibility(View.GONE);
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                            //todo testare
                            BookInfo book = parseDataSnapshotBook(dataSnapshot);
                            if (book == null)
                                return;
                            bookList.remove(book);
                            if (bookList.size() == 0)
                                emptyListMessage.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d(deBugTag, databaseError.getMessage() + databaseError.getCode());
                            networkProblem(databaseError);
                        }
                    };
                    query1.addChildEventListener(childEventListener);
                }
                else if(i==1) {
                    Query query2 = query.orderByChild("bookTitle").equalTo(valueExtra);
                    ChildEventListener childEventListener = new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                            BookInfo book = parseDataSnapshotBook(dataSnapshot);
                            if (book == null)
                                return;
                            bookList.add(book);
                            adapter.notifyDataSetChanged();
                            emptyListMessage.setVisibility(View.GONE);
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                            //todo testare
                            BookInfo book = parseDataSnapshotBook(dataSnapshot);
                            if (book == null)
                                return;
                            bookList.remove(book);
                            if (bookList.size() == 0)
                                emptyListMessage.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d(deBugTag, databaseError.getMessage() + databaseError.getCode());
                            networkProblem(databaseError);
                        }
                    };
                    query2.addChildEventListener(childEventListener);
                }
                else{
                    Query query3 = query.orderByChild("publisher").equalTo(valueExtra);
                    ChildEventListener childEventListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                        BookInfo book = parseDataSnapshotBook(dataSnapshot);
                        if(book == null)
                            return;
                        bookList.add(book);
                        adapter.notifyDataSetChanged();
                        emptyListMessage.setVisibility(View.GONE);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                        //todo testare
                        BookInfo book = parseDataSnapshotBook(dataSnapshot);
                        if(book == null)
                            return;
                        bookList.remove(book);
                        if(bookList.size()==0)
                            emptyListMessage.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(deBugTag,databaseError.getMessage()+databaseError.getCode());
                        networkProblem(databaseError);
                    }
                };
                    query3.addChildEventListener(childEventListener);
                }
            }

        }
    }

    //search of userID passed by the Map activity
    private void searchFromMapMarker(Query query){
        query = query.orderByKey();

        for(String bookId : bookIdList){
            query.equalTo(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    BookInfo book = parseDataSnapshotBook(dataSnapshot.getChildren().iterator().next());
                    if(book == null)
                        return;
                    bookList.add(book);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(deBugTag,databaseError.getMessage());
                    networkProblem(databaseError);
                }
            });
        }

    }

    //search from map markers without a research
    private void searchFromUsers(){
        for(String userId : usersId){
            Query query = FirebaseDatabase.getInstance().getReference().child("books").orderByChild("owner").equalTo(userId);
            query.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName){

                    BookInfo book = parseDataSnapshotBook(dataSnapshot);
                    if(book == null)
                        return;
                    bookList.add(book);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onChildRemoved(DataSnapshot snapshot){
                }

                @Override
                public void onChildChanged(DataSnapshot snapshot, String previousChildName){

                }

                @Override
                public void onChildMoved(DataSnapshot snapshot, String previousChildName){

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(deBugTag,databaseError.getMessage());
                    networkProblem(databaseError);
                }
            });
        }
    }

    public static BookInfo parseDataSnapshotBook(DataSnapshot dataSnapshot){

        BookInfo book = null;

        if(dataSnapshot==null)
            return null;

        if(dataSnapshot.exists()){

            book = new BookInfo();
            book.setBookID(dataSnapshot.getKey());

            for(DataSnapshot child : dataSnapshot.getChildren()){

                switch (child.getKey()){
                    case "ISBN":
                        book.set_ISBN((String)child.getValue());
                        break;
                    case "author":
                        book.setAuthor((String)child.getValue());
                        break;
                    case "bookTitle":
                        book.setBookTitle((String)child.getValue());
                        break;
                    case "editionYear":
                        book.setEditionYear(child.getValue().toString());
                        break;
                    case "owner":
                        book.setOwner((String) child.getValue());
                        break;
                    case "publisher":
                        book.setPublisher((String) child.getValue());
                        break;
                }

            }
        }

        return book;
    }


    private void networkProblem(DatabaseError databaseError){
        if(databaseError.getCode()!=-3){
            Toast.makeText(getApplicationContext(),R.string.network_problem,Toast.LENGTH_SHORT).show();
            onBackPressed();
        }
    }

}
