-if class com.pabl3st.rutapp.data.network.LoginRequest
-keepnames class com.pabl3st.rutapp.data.network.LoginRequest
-if class com.pabl3st.rutapp.data.network.LoginRequest
-keep class com.pabl3st.rutapp.data.network.LoginRequestJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.pabl3st.rutapp.data.network.LoginRequest
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.pabl3st.rutapp.data.network.LoginRequest
-keepclassmembers class com.pabl3st.rutapp.data.network.LoginRequest {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
