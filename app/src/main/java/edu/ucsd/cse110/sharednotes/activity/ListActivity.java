package edu.ucsd.cse110.sharednotes.activity;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import edu.ucsd.cse110.sharednotes.R;
import edu.ucsd.cse110.sharednotes.model.Note;
import edu.ucsd.cse110.sharednotes.model.NoteAPI;
import edu.ucsd.cse110.sharednotes.view.NotesAdapter;
import edu.ucsd.cse110.sharednotes.viewmodel.ListViewModel;

/*
 * Scroll down and try to understand the code yourself before reading this comment.
 * <p>
 * This is an example of what you should aim for your in your own projects. But to do so you need to
 * think about architecture. The purpose of software architecture is to make your code easier to
 * understand and reason about, and therefore less bug prone and easier to maintain.
 * <p>
 * Take a look at this sketch of the architectural diagram.
 * <p>
 * - The solid lines represent direct relationships. - Holds a reference (composes). - Can make
 * direct method calls. - The dotted lines represent indirect relationships. - Can indirectly
 * trigger behavior through a callback or observer.
 * <p>

 * ──Activity ╶╶(observes events on)╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╮    [Presenter - Behavior]
 *   │                                                    ╎
 *   ├──(calls)────→ Repository ←╶╶╶╶╶╮                   ╎    [Model]
 *   │               ├──→ DAO         ╎                   ╎    [Model - Local Backend]
 *   │               │    └──→ DB     ╎                   ╎
 *   │               └──→ API         ╎                   ╎    [Model - Remote Backend]
 *   │                                ╎                   ╎
 *   │               ╭╶╶(observes)╶╶╶╶╯                   ╎
 *   │               ╎                                    ╎
 *   ├──(calls)────→ View Model ←╶╶╶╶╶╮                   ╎    [Presenter - Data]
 *   │                                ╎                   ╎
 *   │               ╭╶╶(observes)╶╶╶╶╯                   ╎
 *   │               ╎                                    ╎
 *   └──(calls)────→ View ←╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╯    [View]
 *                   ├──→ Toolbar                              [View - Subview]
 *                   ├──→ Recycler                             [View - Subview]
 *                   └──→ Input                                [View - Subview]
 *
 *
 * Note 1: You may be wondering where the Note class is... The Note class is "just data", and is
 *         used everywhere. It's a simple POJO. It doesn't depend on anything else, so it doesn't
 *         need to be in the diagram.
 *
 * Note 2: This diagram is a little simplified. In particular, the Adapter is hidden. The Adapter is
 *         responsible for converting the data into a format that the RecyclerView can understand,
 *         so in some sense it's a "mini-Presenter" that mediates between the RecyclerView and the
 *         Activity.
 *
 * Due to Android quirks, we don't exactly have a clean MVP architecture. The Presenter
 * and ViewModel *both* play the role of Presenter, as they both mediate between the View and the
 * Model.
 *
 * How they differ is that the ViewModel is responsible for managing the data, while the Activity is
 * responsible for managing behavior: interactions, events, etc.
 *
 * In addition, the Activity serves as the "god object" because it is the entry point of the app. It
 * also has the responsibility of setting up all of the rest of the MVP architecture.
 */
public class ListActivity extends AppCompatActivity {
    // This annotation will cause an IDE error if you try to access recyclerView outside of a test.
    // It can also be set to "otherwise = VisibleForTesting.PRIVATE" to allow access from this.
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the content view to be the main layout.
        setContentView(R.layout.activity_list);

        // Note: we are avoiding storing viewModel and adapter in fields we access later. This is
        // because fields are mutable, and mutable state is hard to reason about. They could be
        // uninitialized (null), which would mean someone editing this code would have to know
        // which setup methods need to be called before others. This is a recipe for bugs.
        //
        // Instead, we do direct by-parameter dependency inversion/injection.
        // This way, it is impossible to call the setup methods in the wrong order.

        var viewModel = setupViewModel();
        var adapter = setupAdapter(viewModel);

        setupViews(viewModel, adapter);

// testing code
//        NoteAPI myApi = new NoteAPI();
//        var myNote = new MutableLiveData<Note>();
//
//        var executor = Executors.newSingleThreadScheduledExecutor();
//        ScheduledFuture<?> poller;
//
//        poller = executor.scheduleAtFixedRate(() -> {
//            Note retrieveNote;
//            try {
//                retrieveNote = myApi.getNote("sidtest");
//            } catch (Exception e){
//                e.printStackTrace();
//                throw new RuntimeException();
//            }
//            myNote.postValue(retrieveNote);
//
//        }, 0, 3000, TimeUnit.MILLISECONDS);



    }

    private ListViewModel setupViewModel() {
        return new ViewModelProvider(this).get(ListViewModel.class);
    }

    @NonNull
    private NotesAdapter setupAdapter(ListViewModel viewModel) {
        NotesAdapter adapter = new NotesAdapter();
        adapter.setHasStableIds(true);
        adapter.setOnNoteClickListener(note -> onNoteClicked(note, viewModel));
        adapter.setOnNoteDeleteClickListener(note -> onNoteDeleteClicked(note, viewModel));
        viewModel.getNotes().observe(this, adapter::setNotes);
        return adapter;
    }

    private void setupViews(ListViewModel viewModel, NotesAdapter adapter) {
        setupToolbar();
        setupRecycler(adapter);
        setupInput(viewModel);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
    }

    // Override the @VisibleForTesting annotation to allow access from this (and only this) method.
    @SuppressLint("RestrictedApi")
    private void setupRecycler(NotesAdapter adapter) {
        // We store the recycler view in a field _only_ because we will want to access it in tests.
        recyclerView = findViewById(R.id.recycler_main);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);
    }

    private void setupInput(ListViewModel viewModel) {
        var input = (EditText) findViewById(R.id.input_new_note_title);
        input.setOnEditorActionListener((view, actionId, event) -> {
            // If the event isn't "done" or "enter", do nothing.
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return false;
            }

            // Otherwise, create a new note, persist it...
            var title = input.getText().toString();
            var note = viewModel.getOrCreateNote(title);

            // ...wait for the database to finish persisting it...
            note.observe(this, noteEntity -> {
                // ...stop observing.
                note.removeObservers(this);

                // ...and launch NoteActivity with it.
                var intent = NoteActivity.intentFor(this, noteEntity);
                startActivity(intent);
            });

            return true;
        });
    }

    /* Mediation Logic */

    public void onNoteClicked(Note note, ListViewModel viewModel) {
        // Launch NoteActivity
        // Note that we do _NOT_ want to call itemView.getContext().startActivity() inside
        // ViewHolder,because it is the context of a ViewHolder which might get recycled.
        // Therefore, we delegate to the NoteActivity class to create the intent.
        Log.d("NotesAdapter", "Opened note " + note.title);
        var intent = NoteActivity.intentFor(this, note);
        startActivity(intent);
    }

    public void onNoteDeleteClicked(Note note, ListViewModel viewModel) {
        // Delete the note
        Log.d("NotesAdapter", "Deleted note " + note.title);
        viewModel.delete(note);
    }
}