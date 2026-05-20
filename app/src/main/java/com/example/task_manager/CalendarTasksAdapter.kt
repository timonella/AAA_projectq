package com.example.task_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class CalendarTasksAdapter(
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<CalendarTasksAdapter.TaskViewHolder>() {

    private var tasks: List<Task> = emptyList()

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
        holder.itemView.setOnClickListener { onTaskClick(task) }
    }

    override fun getItemCount(): Int = tasks.size

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskNameText: TextView = itemView.findViewById(R.id.taskNameText)
        private val taskTimeText: TextView = itemView.findViewById(R.id.taskTimeText)
        private val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator)
        private val categoryText: TextView = itemView.findViewById(R.id.categoryText)
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        private val cardView: CardView = itemView.findViewById(R.id.taskCard)
        private val statusIcon: View = itemView.findViewById(R.id.statusIcon)

        fun bind(task: Task) {
            taskNameText.text = task.name

            // Отображение времени
            if (task.dueTime != null && task.dueTime.isNotEmpty()) {
                taskTimeText.text = task.dueTime
                taskTimeText.visibility = View.VISIBLE
            } else {
                taskTimeText.visibility = View.GONE
            }

            // Отображение категории
            categoryText.text = task.category

            // Отображение описания
            if (task.description.isNotEmpty()) {
                descriptionText.text = task.description
                descriptionText.visibility = View.VISIBLE
            } else {
                descriptionText.visibility = View.GONE
            }

            // Установка цвета приоритета
            when (task.priority) {
                Priority.LOW -> {
                    priorityIndicator.setBackgroundColor(
                        itemView.context.getColor(android.R.color.holo_green_light)
                    )
                }
                Priority.MEDIUM -> {
                    priorityIndicator.setBackgroundColor(
                        itemView.context.getColor(android.R.color.holo_orange_light)
                    )
                }
                Priority.HIGH -> {
                    priorityIndicator.setBackgroundColor(
                        itemView.context.getColor(android.R.color.holo_red_light)
                    )
                }
            }

            // Отметка о выполнении
            if (task.isCompleted) {
                taskNameText.alpha = 0.5f
                cardView.alpha = 0.7f
                statusIcon.setBackgroundColor(
                    itemView.context.getColor(android.R.color.holo_green_dark)
                )
            } else {
                taskNameText.alpha = 1f
                cardView.alpha = 1f
                statusIcon.setBackgroundColor(
                    itemView.context.getColor(android.R.color.darker_gray)
                )
            }
        }
    }
}