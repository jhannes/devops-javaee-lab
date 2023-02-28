package com.soprasteria.devopsacademy;

import com.soprasteria.devopsacademy.generated.api.SampleModelData;
import com.soprasteria.devopsacademy.generated.api.TodoItemDto;
import com.soprasteria.infrastructure.TestDatabase;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@TestDatabase
class TodoApiTest {

    private final SampleModelData sampleModelData = new SampleModelData(System.currentTimeMillis());
    private final TodoApi api = new TodoApi();

    @Test
    void shouldRetrieveInsertedItems() throws SQLException {
        var item = sampleModelData.sampleTodoItemDto();
        api.createTodoItem(item);
        assertThat(api.listTodos())
                .extracting(TodoItemDto::getId)
                .contains(item.getId());
    }

    @Test
    void shouldSaveAllProperties() throws SQLException {
        var item = sampleModelData.sampleTodoItemDto();
        api.createTodoItem(item);
        assertThat(api.getTodo(item.getId()))
                .hasNoNullFieldsOrProperties()
                .usingRecursiveAssertion()
                .isEqualTo(item);
    }


}