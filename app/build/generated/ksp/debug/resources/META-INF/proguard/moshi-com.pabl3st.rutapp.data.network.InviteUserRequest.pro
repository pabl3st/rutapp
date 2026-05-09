-if class com.pabl3st.rutapp.data.network.InviteUserRequest
-keepnames class com.pabl3st.rutapp.data.network.InviteUserRequest
-if class com.pabl3st.rutapp.data.network.InviteUserRequest
-keep class com.pabl3st.rutapp.data.network.InviteUserRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.pabl3st.rutapp.data.network.InviteUserRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.pabl3st.rutapp.data.network.InviteUserRequest
-keepclassmembers class com.pabl3st.rutapp.data.network.InviteUserRequest {
    public synthetic <init>(java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
