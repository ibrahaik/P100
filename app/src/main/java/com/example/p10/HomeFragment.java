package com.example.p10;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.p10.R;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;


public class HomeFragment extends Fragment {

    // Argumentos para inicialización del fragmento
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    AppViewModel appViewModel;

    private NavController navController;
    PostsAdapter adapter;
    private String mParam1;
    private String mParam2;

    private ImageView photoImageView;
    private TextView displayNameTextView, emailTextView;
    private Client client;
    private Account account;
    private String userId;

    public HomeFragment() {
        // Constructor vacío requerido
    }


    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appViewModel = new
                ViewModelProvider(requireActivity()).get(AppViewModel.class);

        // Obtener referencias a los elementos del encabezado del NavigationView
        NavigationView navigationView = view.getRootView().findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        photoImageView = header.findViewById(R.id.imageView);
        displayNameTextView = header.findViewById(R.id.displayNameTextView);
        emailTextView = header.findViewById(R.id.emailTextView);

        navController = Navigation.findNavController(view);

        // Inicializar Appwrite Client
        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Obtener información del usuario
        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                mainHandler.post(() -> {
                    userId = result.getId();
                    displayNameTextView.setText(result.getName());
                    emailTextView.setText(result.getEmail());
                    Glide.with(requireView()).load(R.drawable.user).into(photoImageView);
                    obtenerPosts();
                });
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.newPostFragment);
            }
        });

        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        adapter = new PostsAdapter();
        postsRecyclerView.setAdapter(adapter);


    }

    // Dentro de HomeFragment.java

    class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView authorPhotoImageView, likeImageView, mediaImageView, btnDelete, btnShare;
        TextView authorTextView, contentTextView, numLikesTextView;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorPhotoImageView = itemView.findViewById(R.id.authorPhotoImageView);
            likeImageView = itemView.findViewById(R.id.likeImageView);
            mediaImageView = itemView.findViewById(R.id.mediaImage);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
            btnDelete = itemView.findViewById(R.id.btnDelete); // Referencia al botón de eliminar
            btnShare = itemView.findViewById(R.id.btnShare);
        }
    }

    class PostsAdapter extends RecyclerView.Adapter<PostViewHolder> {
        DocumentList<Map<String, Object>> lista = null;

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.viewholder_post, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Map<String, Object> post = lista.getDocuments().get(position).getData();

            // Configurar datos del post (autor, contenido, etc.)
            if (post.get("authorPhotoUrl") == null) {
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
            } else {
                Glide.with(getContext()).load(post.get("authorPhotoUrl").toString()).circleCrop()
                        .into(holder.authorPhotoImageView);
            }
            holder.authorTextView.setText(post.get("author").toString());
            // Mostrar contenido con hashtags en azul (fake visual)
            String mensaje = post.get("content") != null ? post.get("content").toString() : "";
            SpannableString spannable = new SpannableString(mensaje);
            String[] palabras = mensaje.split(" ");
            for (String palabra : palabras) {
                if (palabra.startsWith("#")) {
                    int start = mensaje.indexOf(palabra);
                    int end = start + palabra.length();
                    spannable.setSpan(new ForegroundColorSpan(Color.BLUE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            holder.contentTextView.setText(spannable);


            // Gestión de likes (código existente)
            List<String> likes = (List<String>) post.get("likes");
            if (likes.contains(userId))
                holder.likeImageView.setImageResource(R.drawable.like_on);
            else
                holder.likeImageView.setImageResource(R.drawable.like_off);
            holder.numLikesTextView.setText(String.valueOf(likes.size()));
            holder.likeImageView.setOnClickListener(view -> {
                Databases databases = new Databases(client);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                List<String> nuevosLikes = likes;
                if(nuevosLikes.contains(userId))
                    nuevosLikes.remove(userId);
                else
                    nuevosLikes.add(userId);
                Map<String, Object> data = new HashMap<>();
                data.put("likes", nuevosLikes);
                try {

                    databases.updateDocument(
                            getString(R.string.APPWRITE_DATABASE_ID),
                            getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                            post.get("$id").toString(), // documentId
                            data, // data (optional)
                            new ArrayList<>(), // permissions (optional)
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    error.printStackTrace();
                                    return;
                                }
                                System.out.println("Likes actualizados:" +
                                        result.toString());
                                mainHandler.post(() -> obtenerPosts());
                            })
                    );
                } catch (AppwriteException e) {
                    throw new RuntimeException(e);
                }
            });

            // Configuración del botón eliminar
            if (post.get("uid") != null && post.get("uid").toString().equals(userId)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(view -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar Post")
                        .setMessage("¿Seguro que deseas eliminar este post?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            // Se crea una copia del post para poder restaurarlo en caso de "deshacer"
                            final Map<String, Object> deletedPost = new HashMap<>(post);
                            String postId = post.get("$id").toString();
                            DeletePost.deletePost(client, postId, requireContext(), success -> {
                                if (success) {
                                    // Actualizamos la lista de posts
                                    obtenerPosts();
                                    // Mostrar Snackbar con acción "Deshacer"
                                    Snackbar.make(requireView(), "Post eliminado", Snackbar.LENGTH_LONG)
                                            .setAction("Deshacer", v -> {
                                                DeletePost.restorePost(client, deletedPost, requireContext(), restoreSuccess -> {
                                                    if (restoreSuccess) {
                                                        obtenerPosts();
                                                        Snackbar.make(requireView(), "Post restaurado", Snackbar.LENGTH_SHORT).show();
                                                    } else {
                                                        Snackbar.make(requireView(), "Error al restaurar el post", Snackbar.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }).show();
                                } else {
                                    Snackbar.make(requireView(), "Error al eliminar el post", Snackbar.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

            // Configuración de media (código existente)
            if (post.get("mediaUrl") != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.get("mediaType").toString())) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.get("mediaUrl").toString()).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }



            holder.btnShare.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");

                String contenido = post.get("content") != null ? post.get("content").toString() : "Sin contenido";
                shareIntent.putExtra(Intent.EXTRA_TEXT, contenido);

                Intent chooser = Intent.createChooser(shareIntent, "Compartir publicación");
                v.getContext().startActivity(chooser);
            });

        }

        @Override
        public int getItemCount() {
            return lista == null ? 0 : lista.getDocuments().size();
        }

        public void establecerLista(DocumentList<Map<String, Object>> lista) {
            this.lista = lista;
            notifyDataSetChanged();
        }
    }


    void obtenerPosts()
    {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID), // collectionId
                    new ArrayList<>(), // queries (optional)
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los posts: "
                                    + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        System.out.println( result.toString() );
                        mainHandler.post(() -> adapter.establecerLista(result));
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }
}