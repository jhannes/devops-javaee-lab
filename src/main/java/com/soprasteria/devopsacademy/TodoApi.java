package com.soprasteria.devopsacademy;

import com.soprasteria.devopsacademy.generated.api.TodoItemDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/todos")
public class TodoApi {

    @POST
    public void createTodoItem(TodoItemDto item) throws SQLException {
        try (var statement = ApplicationDataSource.currentConnection().prepareStatement("insert into todo_items (id, title, body) values (?, ?, ?) returning *")) {
            statement.setObject(1, item.getId());
            statement.setString(2, item.getTitle());
            statement.setString(3, item.getDescription());
            statement.execute();

            var rs = statement.getResultSet();
            if (rs.next()) {
                item.setCreated(OffsetDateTime.ofInstant(
                        rs.getTimestamp("created_at").toInstant(),
                        ZoneId.systemDefault()
                ));
            }
        }
    }

    @GET
    public List<TodoItemDto> listTodos() throws SQLException {
        try (var statement = ApplicationDataSource.currentConnection().prepareStatement("select * from todo_items")) {
            try (var rs = statement.executeQuery()) {
                var result = new ArrayList<TodoItemDto>();
                while (rs.next()) {
                    result.add(readTodoItem(rs));
                }
                return result;
            }
        }
    }

    public TodoItemDto getTodo(UUID id) throws SQLException {
        try (var statement = ApplicationDataSource.currentConnection().prepareStatement("select * from todo_items where id = ?")) {
            statement.setObject(1, id);
            try (var rs = statement.executeQuery()) {
                if (rs.next()) {
                    return readTodoItem(rs);
                }
                throw new IllegalArgumentException("No todo with id " + id);
            }
        }
    }

    private TodoItemDto readTodoItem(ResultSet rs) throws SQLException {
        return new TodoItemDto()
                .id((UUID) rs.getObject("id"))
                .title(rs.getString("title"))
                .description(rs.getString("body"))
                .created(OffsetDateTime.ofInstant(
                        rs.getTimestamp("created_at").toInstant(),
                        ZoneId.systemDefault()
                ));
    }

}
