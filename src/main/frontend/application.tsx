import * as React from "react";
import { FormEvent, useState } from "react";
import {
  servers,
  TodoItemDto,
} from "../../../target/generated-sources/openapi";
import { useLoader } from "./useLoader";
import { v4 as uuidv4 } from "uuid";

function ErrorView({ error }: { error: Error }) {
  console.error(error);
  return (
    <>
      <h4>An error occurred</h4>
      <div>{error.toString()}</div>
    </>
  );
}

function ListTodoItems() {
  const { loading, values, error } = useLoader(() =>
    servers.default.todosApi.listTodos()
  );

  if (error) {
    return <ErrorView error={error} />;
  }

  if (loading || !values) {
    return <div>Loading...</div>;
  }

  return (
    <>
      {values.map((t) => (
        <SingleTodoItem key={t.id} item={t} />
      ))}
    </>
  );
}

function SingleTodoItem({ item }: { item: TodoItemDto }) {
  return (
    <div key={item.id}>
      <h2>{item.title}</h2>
      <div>{item.description}</div>
    </div>
  );
}

function CreateTodoItem() {
  const [todoItemDto, setTodoItemDto] = useState<TodoItemDto>({
    title: "",
    id: uuidv4(),
    description: "",
  });

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    await servers.default.todosApi.createTodo({ todoItemDto });
    setTodoItemDto({ title: "", id: uuidv4(), description: "" });
  }

  return (
    <form onSubmit={handleSubmit}>
      <div>
        <label>
          Title:{" "}
          <input
            value={todoItemDto.title}
            onChange={(e) =>
              setTodoItemDto((old) => ({ ...old, title: e.target.value }))
            }
          />
        </label>
      </div>
      <div>
        <label htmlFor="body">Body:</label>
      </div>
      <textarea
        id="body"
        value={todoItemDto.description}
        onChange={(e) =>
          setTodoItemDto((old) => ({ ...old, description: e.target.value }))
        }
      />
      <div>
        <button>Submit</button>
      </div>
    </form>
  );
}

export function Application() {
  return (
    <>
      <h1>Todo Application</h1>
      <ListTodoItems />

      <h1>Make new todo</h1>
      <CreateTodoItem />
    </>
  );
}
