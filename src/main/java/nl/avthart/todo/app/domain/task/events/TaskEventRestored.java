package nl.avthart.todo.app.domain.task.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskEventRestored implements TaskEvent {

    String id;
}
