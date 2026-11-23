package id.neotica.orpheum.source;

public interface SourceCallback<T> {

    void onSuccess(T data);

    void onError(String message);
}
