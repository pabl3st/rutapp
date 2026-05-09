-if class com.pabl3st.rutapp.data.network.BatchSyncResult
-keepnames class com.pabl3st.rutapp.data.network.BatchSyncResult
-if class com.pabl3st.rutapp.data.network.BatchSyncResult
-keep class com.pabl3st.rutapp.data.network.BatchSyncResultJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.pabl3st.rutapp.data.network.BatchSyncResult
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.pabl3st.rutapp.data.network.BatchSyncResult
-keepclassmembers class com.pabl3st.rutapp.data.network.BatchSyncResult {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.Integer,boolean,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
