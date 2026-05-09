-if class com.pabl3st.rutapp.data.network.ApiError
-keepnames class com.pabl3st.rutapp.data.network.ApiError
-if class com.pabl3st.rutapp.data.network.ApiError
-keep class com.pabl3st.rutapp.data.network.ApiErrorJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
