-if class com.pabl3st.rutapp.data.network.AccountUserDto
-keepnames class com.pabl3st.rutapp.data.network.AccountUserDto
-if class com.pabl3st.rutapp.data.network.AccountUserDto
-keep class com.pabl3st.rutapp.data.network.AccountUserDtoJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.pabl3st.rutapp.data.network.AccountUserDto
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.pabl3st.rutapp.data.network.AccountUserDto
-keepclassmembers class com.pabl3st.rutapp.data.network.AccountUserDto {
    public synthetic <init>(int,java.lang.String,java.lang.String,java.lang.String,java.lang.String,boolean,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
