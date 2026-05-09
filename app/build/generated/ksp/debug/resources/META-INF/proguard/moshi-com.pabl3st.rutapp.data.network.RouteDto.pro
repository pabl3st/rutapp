-if class com.pabl3st.rutapp.data.network.RouteDto
-keepnames class com.pabl3st.rutapp.data.network.RouteDto
-if class com.pabl3st.rutapp.data.network.RouteDto
-keep class com.pabl3st.rutapp.data.network.RouteDtoJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.pabl3st.rutapp.data.network.RouteDto
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.pabl3st.rutapp.data.network.RouteDto
-keepclassmembers class com.pabl3st.rutapp.data.network.RouteDto {
    public synthetic <init>(java.lang.Integer,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int,int,java.lang.String,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
