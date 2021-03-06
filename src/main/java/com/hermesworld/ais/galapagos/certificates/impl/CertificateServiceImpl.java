package com.hermesworld.ais.galapagos.certificates.impl;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS12PfxPduBuilder;
import org.bouncycastle.pkcs.PKCS12SafeBag;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.bc.BcPKCS12MacCalculatorBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS12SafeBagBuilder;
import org.springframework.stereotype.Component;

import com.hermesworld.ais.galapagos.certificates.CaManager;
import com.hermesworld.ais.galapagos.certificates.CertificateService;
import com.hermesworld.ais.galapagos.kafka.config.KafkaEnvironmentConfig;

@Component
public class CertificateServiceImpl implements CertificateService {

	private static final ASN1ObjectIdentifier TRUSTED_KEY_USAGE = new ASN1ObjectIdentifier("2.16.840.1.113894.746875.1.1");
	private static final ASN1ObjectIdentifier ANY_EXTENDED_KEY_USAGE = new ASN1ObjectIdentifier("2.5.29.37.0");

	private byte[] truststore;

	@Override
	public CaManager buildCaManager(KafkaEnvironmentConfig env, File certificatesWorkdir)
			throws IOException, GeneralSecurityException, OperatorCreationException {
		return new CaManagerImpl(env, certificatesWorkdir);
	}

	@Override
	public void buildTrustStore(Map<String, CaManager> caManagers) throws IOException, GeneralSecurityException, PKCSException {
		PKCS12PfxPduBuilder keyStoreBuilder = new PKCS12PfxPduBuilder();

		for (Map.Entry<String, CaManager> entry : caManagers.entrySet()) {
			keyStoreBuilder.addData(new JcaPKCS12SafeBagBuilder(entry.getValue().getCaCertificate())
					.addBagAttribute(PKCS12SafeBag.friendlyNameAttribute, new DERBMPString("kafka_" + entry.getKey() + "_ca"))
					.addBagAttribute(TRUSTED_KEY_USAGE, ANY_EXTENDED_KEY_USAGE).build());
		}

		this.truststore = keyStoreBuilder.build(new BcPKCS12MacCalculatorBuilder(), "changeit".toCharArray())
				.getEncoded(ASN1Encoding.DL);
	}

	@Override
	public byte[] getTrustStorePkcs12() {
		return truststore;
	}

}
