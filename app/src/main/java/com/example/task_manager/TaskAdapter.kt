package com.example.task_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val onTaskStatusChanged: (Task, Int) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun getItemCount(): Int {
        val count = tasks.size
        println("=== АДАПТЕР: getItemCount() = $count ===")
        return count
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        println("=== АДАПТЕР: создаю ViewHolder ===")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        println("=== АДАПТЕР: привязываю позицию $position ===")
        val task = tasks[position]
        println("Задача: ${task.name}")
        holder.bind(task, position)
    }

    fun updateTasks(newTasks: List<Task>) {
        println("TaskAdapter.updateTasks() вызван с ${newTasks.size} задачами")
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
        println("После notifyDataSetChanged, задач в адаптере: ${tasks.size}")
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.taskCheckBox)
        private val taskNameTextView: TextView = itemView.findViewById(R.id.taskNameTextView)
        private val taskDescriptionTextView: TextView = itemView.findViewById(R.id.taskDescriptionTextView)
        private val taskCategoryTextView: TextView = itemView.findViewById(R.id.taskCategoryTextView)
        private val taskReminderTextView: TextView = itemView.findViewById(R.id.taskReminderTextView)

        fun bind(task: Task, position: Int) {
            checkBox.setOnCheckedChangeListener(null)

            taskNameTextView.text = task.name
            checkBox.isChecked = task.isCompleted

            if (task.description.isNotEmpty()) {
                taskDescriptionTextView.text = task.description
                taskDescriptionTextView.visibility = View.VISIBLE
            } else {
                taskDescriptionTextView.visibility = View.GONE
            }

            taskCategoryTextView.text = "${task.category}"
            taskReminderTextView.text = "${task.reminderMinutesBefore}"

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val updatedTask = task.copy(isCompleted = isChecked)
                onTaskStatusChanged(updatedTask, position)
            }
        }
    }
}
