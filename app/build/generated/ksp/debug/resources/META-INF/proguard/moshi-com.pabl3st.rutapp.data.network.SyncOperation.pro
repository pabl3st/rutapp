-if class com.pabl3st.rutapp.data.network.SyncOperation
-keepnames class com.pabl3st.rutapp.data.network.SyncOperation
-if class com.pabl3st.rutapp.data.network.SyncOperation
-keep class com.pabl3st.rutapp.data.network.SyncOperationJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
