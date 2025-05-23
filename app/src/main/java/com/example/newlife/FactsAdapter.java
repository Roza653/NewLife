//package com.example.newlife;
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import java.util.List;
//
//public class FactsAdapter extends RecyclerView.Adapter<FactsAdapter.FactsViewHolder> {
//
//    private Context context;
//    private List<Person> personList;
//
//    public FactsAdapter(Context context, List<Person> personList) {
//        this.context = context;
//        this.personList = personList;
//    }
//
//    @NonNull
//    @Override
//    public FactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.item_person, parent, false);
//        return new FactsViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull FactsViewHolder holder, int position) {
//        Person person = personList.get(position);
//        holder.nameTextView.setText(person.getName());
//        holder.habitTextView.setText(person.getHabit());
//    }
//
//    @Override
//    public int getItemCount() {
//        return personList.size();
//    }
//
//    public static class FactsViewHolder extends RecyclerView.ViewHolder {
//        TextView nameTextView, habitTextView;
//
//        public FactsViewHolder(@NonNull View itemView) {
//            super(itemView);
//            nameTextView = itemView.findViewById(R.id.nameTextView);
//            habitTextView = itemView.findViewById(R.id.habitTextView);
//        }
//    }
//}
package com.example.newlife;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FactsAdapter extends RecyclerView.Adapter<FactsAdapter.FactsViewHolder> {

    private Context context;
    private List<Person> personList;

    public FactsAdapter(Context context, List<Person> personList) {
        this.context = context;
        this.personList = personList;
    }

    @NonNull
    @Override
    public FactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_habit, parent, false);
        return new FactsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FactsViewHolder holder, int position) {
        Person person = personList.get(position);
        holder.habitTextView.setText(person.getName() + ": " + person.getHabit());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, HomeActivity.class);
            intent.putExtra("habit_name", person.getName());
            intent.putExtra("habit_detail", person.getHabit());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return personList.size();
    }

    public static class FactsViewHolder extends RecyclerView.ViewHolder {
        TextView habitTextView;

        public FactsViewHolder(@NonNull View itemView) {
            super(itemView);
            habitTextView = itemView.findViewById(R.id.habitTextView);
        }
    }
}