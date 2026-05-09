-if class com.pabl3st.rutapp.data.network.DeltaSyncResponse
-keepnames class com.pabl3st.rutapp.data.network.DeltaSyncResponse
-if class com.pabl3st.rutapp.data.network.DeltaSyncResponse
-keep class com.pabl3st.rutapp.data.network.DeltaSyncResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.pabl3st.rutapp.data.network.DeltaSyncResponse
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.pabl3st.rutapp.data.network.DeltaSyncResponse
-keepclassmembers class com.pabl3st.rutapp.data.network.DeltaSyncResponse {
    public synthetic <init>(boolean,java.util.List,java.util.List,java.util.List,java.util.List,java.lang.String,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
