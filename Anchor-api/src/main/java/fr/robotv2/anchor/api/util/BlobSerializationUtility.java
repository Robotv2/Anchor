package fr.robotv2.anchor.api.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Utility class for serializing and deserializing blob fields.
 * <p>
 * This class provides methods to convert Serializable objects to byte arrays for storage
 * in BLOB database columns and convert them back to their original form using Java serialization.
 * </p>
 *
 * @since 1.0
 */
public final class BlobSerializationUtility {

    private BlobSerializationUtility() {
        throw new UnsupportedOperationException("This class is a utility class and cannot be instantiated");
    }

    /**
     * Serializes a Serializable object to a byte array for BLOB storage.
     * <p>
     * This method uses Java serialization to convert the object to a byte array.
     * The object must implement {@link Serializable}.
     * </p>
     *
     * @param object the Serializable object to serialize, may be {@code null}
     * @return byte array representation of the object, or {@code null} if input is null
     * @throws IllegalArgumentException if object is not Serializable or serialization fails
     */
    public static byte @Nullable [] serialize(@Nullable Object object) {
        if (object == null) {
            return null;
        }

        if (!(object instanceof Serializable)) {
            throw new IllegalArgumentException("Object must implement Serializable to be used as a blob field: " + object.getClass().getName());
        }

        try {
            return javaSerialize((Serializable) object);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to serialize object of type " + object.getClass().getName(), e);
        }
    }

    /**
     * Deserializes a byte array from BLOB storage back to an object.
     * <p>
     * This method uses Java deserialization to convert the byte array back to the original object.
     * </p>
     *
     * @param bytes the byte array to deserialize, may be {@code null}
     * @param targetType the target class to deserialize to, must not be {@code null}
     * @return the deserialized object, or {@code null} if input is null
     * @throws IllegalArgumentException if deserialization fails or the deserialized object is not of the expected type
     */
    @Nullable
    public static <T> T deserialize(@Nullable byte[] bytes, @NotNull Class<T> targetType) {
        if (bytes == null) {
            return null;
        }

        try {
            Object obj = javaDeserialize(bytes);
            if (targetType.isInstance(obj)) {
                return targetType.cast(obj);
            } else {
                throw new IllegalArgumentException("Deserialized object is of type " + obj.getClass().getName() + " but expected type " + targetType.getName());
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Failed to deserialize byte array to type " + targetType.getName(), e);
        }
    }

    /**
     * Serializes a Serializable object using Java serialization.
     *
     * @param object the Serializable object to serialize, must not be {@code null}
     * @return byte array representation of the object
     * @throws IOException if serialization fails
     */
    private static byte[] javaSerialize(@NotNull Serializable object) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return baos.toByteArray();
        }
    }

    /**
     * Deserializes a byte array using Java deserialization.
     *
     * @param bytes the byte array to deserialize, must not be {@code null}
     * @return the deserialized object
     * @throws IOException if deserialization fails
     * @throws ClassNotFoundException if the class of the serialized object cannot be found
     */
    private static Object javaDeserialize(@NotNull byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }
}