-if class com.pabl3st.rutapp.data.network.BaseResponse
-keepnames class com.pabl3st.rutapp.data.network.BaseResponse
-if class com.pabl3st.rutapp.data.network.BaseResponse
-keep class com.pabl3st.rutapp.data.network.BaseResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.pabl3st.rutapp.data.network.BaseResponse
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.pabl3st.rutapp.data.network.BaseResponse
-keepclassmembers class com.pabl3st.rutapp.data.network.BaseResponse {
    public synthetic <init>(boolean,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
