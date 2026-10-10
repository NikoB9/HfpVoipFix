import com.android.apksig.ApkSigner;
import com.android.apksig.util.DataSources;
import com.android.apksig.util.DataSinks;
import com.android.apksig.util.ReadableDataSink;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

/** Uses Google's apksig, then performs a single sequential, synced file write.
 * Avoids random-access output inconsistencies in virtual filesystems. No private values are logged. */
public final class SignApk {
    public static void main(String[] args)throws Exception{
        String password=System.getenv("LAB_KEYSTORE_PASSWORD");
        if(password==null||password.isEmpty())throw new IllegalStateException("Missing signing password");
        KeyStore store=KeyStore.getInstance("PKCS12");
        try(InputStream in=Files.newInputStream(Path.of(System.getenv("LAB_KEYSTORE")))){store.load(in,password.toCharArray());}
        PrivateKey key=(PrivateKey)store.getKey("hfplab",password.toCharArray());
        X509Certificate cert=(X509Certificate)store.getCertificate("hfplab");
        if(key==null||cert==null)throw new IllegalStateException("Missing hfplab identity");
        ApkSigner.SignerConfig config=new ApkSigner.SignerConfig.Builder("HFPLAB",new com.android.apksig.KeyConfig.Jca(key),Collections.singletonList(cert)).build();
        ReadableDataSink sink=DataSinks.newInMemoryDataSink();
        new ApkSigner.Builder(Collections.singletonList(config))
            .setInputApk(DataSources.asDataSource(ByteBuffer.wrap(Files.readAllBytes(Path.of(args[0])))))
            .setOutputApk(sink).setMinSdkVersion(30)
            .setV1SigningEnabled(false).setV2SigningEnabled(true).setV3SigningEnabled(true).setV4SigningEnabled(false)
            .setAlignmentPreserved(true).build().sign();
        try(FileOutputStream out=new FileOutputStream(args[1])){sink.feed(0,sink.size(),DataSinks.asDataSink(out));out.getFD().sync();}
    }
}
