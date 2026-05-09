-if class com.pabl3st.rutapp.data.network.UsersListResponse
-keepnames class com.pabl3st.rutapp.data.network.UsersListResponse
-if class com.pabl3st.rutapp.data.network.UsersListResponse
-keep class com.pabl3st.rutapp.data.network.UsersListResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.pabl3st.rutapp.data.network.UsersListResponse
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.pabl3st.rutapp.data.network.UsersListResponse
-keepclassmembers class com.pabl3st.rutapp.data.network.UsersListResponse {
    public synthetic <init>(boolean,java.util.List,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
