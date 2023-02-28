import { useEffect, useState } from "react";

export function useLoader<T>(loader: () => Promise<T>) {
  const [loading, setLoading] = useState(true);
  const [values, setValues] = useState<T>();
  const [error, setError] = useState<Error | undefined>();

  async function load() {
    setLoading(true);
    setValues(undefined);
    setError(undefined);
    try {
      setValues(await loader());
    } catch (e) {
      setError(e as Error);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  return { loading, values, error };
}
