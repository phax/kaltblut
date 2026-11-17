/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.kaltblut.core.inspect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.kaltblut.core.model.HybridXmlContext;
import com.helger.kaltblut.core.source.HybridSource;
import com.helger.kaltblut.testfiles.KaltblutTestFiles;

/**
 * Test class for class {@link HybridXmlContextReader}.
 *
 * @author Alexandre Gabriel Carvalho Silva
 */
public final class HybridXmlContextReaderTest
{
  private static final String [] ALL_SAMPLES = { KaltblutTestFiles.ZF_2_0_1_EN16931,
                                                 KaltblutTestFiles.ZF_2_1_EN16931_EMBEDDED,
                                                 KaltblutTestFiles.ZF_2_2_XRECHNUNG,
                                                 KaltblutTestFiles.ZF_2_3_MINIMUM,
                                                 KaltblutTestFiles.ZF_2_4_BASIC_WL };

  @NonNull
  private static HybridXmlContext _read (@NonNull final String sXml)
  {
    return HybridXmlContextReader.readXmlContext (sXml.getBytes (StandardCharsets.UTF_8));
  }

  /**
   * Build a minimal CII document.
   */
  @NonNull
  private static String _cii (@Nullable final String sSellerVat,
                              @Nullable final String sSellerPostal,
                              @Nullable final String sTaxRepVat)
  {
    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("<rsm:CrossIndustryInvoice xmlns:rsm='urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100'" +
                " xmlns:ram='urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100'>");
    aSB.append ("<rsm:ExchangedDocumentContext>" +
                "<ram:BusinessProcessSpecifiedDocumentContextParameter><ram:ID>A1</ram:ID></ram:BusinessProcessSpecifiedDocumentContextParameter>" +
                "<ram:GuidelineSpecifiedDocumentContextParameter><ram:ID> urn:cen.eu:en16931:2017 </ram:ID></ram:GuidelineSpecifiedDocumentContextParameter>" +
                "</rsm:ExchangedDocumentContext>");
    aSB.append ("<rsm:SupplyChainTradeTransaction><ram:ApplicableHeaderTradeAgreement><ram:SellerTradeParty>");
    if (sSellerPostal != null)
      aSB.append ("<ram:PostalTradeAddress><ram:CountryID>" +
                  sSellerPostal +
                  "</ram:CountryID></ram:PostalTradeAddress>");
    // A non-VAT registration first, to check that schemeID filtering works
    aSB.append ("<ram:SpecifiedTaxRegistration><ram:ID schemeID='FC'>201/113/40209</ram:ID></ram:SpecifiedTaxRegistration>");
    if (sSellerVat != null)
      aSB.append ("<ram:SpecifiedTaxRegistration><ram:ID schemeID='VA'>" +
                  sSellerVat +
                  "</ram:ID></ram:SpecifiedTaxRegistration>");
    aSB.append ("</ram:SellerTradeParty>");
    if (sTaxRepVat != null)
      aSB.append ("<ram:SellerTaxRepresentativeTradeParty><ram:SpecifiedTaxRegistration><ram:ID schemeID='VA'>" +
                  sTaxRepVat +
                  "</ram:ID></ram:SpecifiedTaxRegistration></ram:SellerTaxRepresentativeTradeParty>");
    aSB.append ("</ram:ApplicableHeaderTradeAgreement></rsm:SupplyChainTradeTransaction></rsm:CrossIndustryInvoice>");
    return aSB.toString ();
  }

  /**
   * Build a minimal UBL document.
   */
  @NonNull
  private static String _ubl (@NonNull final String sRoot,
                              @Nullable final String sSellerVat,
                              @Nullable final String sSellerPostal,
                              @Nullable final String sTaxRepVat)
  {
    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("<" +
                sRoot +
                " xmlns='urn:oasis:names:specification:ubl:schema:xsd:" +
                sRoot +
                "-2'" +
                " xmlns:cac='urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2'" +
                " xmlns:cbc='urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2'>");
    aSB.append ("<cbc:CustomizationID>urn:cen.eu:en16931:2017#compliant#urn:xeinkauf.de:kosit:xrechnung_3.0</cbc:CustomizationID>");
    aSB.append ("<cbc:ProfileID>urn:fdc:peppol.eu:2017:poacc:billing:01:1.0</cbc:ProfileID>");
    aSB.append ("<cac:AccountingSupplierParty><cac:Party>");
    if (sSellerPostal != null)
      aSB.append ("<cac:PostalAddress><cac:Country><cbc:IdentificationCode>" +
                  sSellerPostal +
                  "</cbc:IdentificationCode></cac:Country></cac:PostalAddress>");
    // A non-VAT tax scheme first, to check that TaxScheme filtering works
    aSB.append ("<cac:PartyTaxScheme><cbc:CompanyID>201/113/40209</cbc:CompanyID><cac:TaxScheme><cbc:ID>FC</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme>");
    if (sSellerVat != null)
      aSB.append ("<cac:PartyTaxScheme><cbc:CompanyID>" +
                  sSellerVat +
                  "</cbc:CompanyID><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme>");
    aSB.append ("</cac:Party></cac:AccountingSupplierParty>");
    if (sTaxRepVat != null)
      aSB.append ("<cac:TaxRepresentativeParty><cac:PartyTaxScheme><cbc:CompanyID>" +
                  sTaxRepVat +
                  "</cbc:CompanyID><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme></cac:TaxRepresentativeParty>");
    aSB.append ("</" + sRoot + ">");
    return aSB.toString ();
  }

  @Test
  public void testAllSamples () throws IOException
  {
    // All shipped samples are German FeRD samples with seller VAT ID DE123456789
    for (final String sPath : ALL_SAMPLES)
    {
      final HybridXmlContext aCtx = HybridXmlContextReader.readXmlContext (HybridSource.fromClasspath (sPath));
      assertNotNull (aCtx);
      assertEquals (sPath, HybridXmlContext.SYNTAX_CII, aCtx.getSyntax ());
      assertNotNull (sPath, aCtx.getCustomizationID ());
      assertEquals (sPath, "DE", aCtx.getSellerVatCountryCode ());
      assertEquals (sPath, "DE", aCtx.getSellerPostalCountryCode ());
      assertEquals (sPath, EZugferdCountry.DE, aCtx.getResolvedCountry ());
      // The MINIMUM sample has no buyer country
      assertEquals (sPath,
                    sPath.equals (KaltblutTestFiles.ZF_2_3_MINIMUM) ? EZugferdCountry.OTHER : EZugferdCountry.DE,
                    aCtx.getResolvedBuyerCountry ());
    }
  }

  @Test
  public void testCII ()
  {
    final HybridXmlContext aCtx = _read (_cii ("FR12345678901", "FR", null));
    assertEquals (HybridXmlContext.SYNTAX_CII, aCtx.getSyntax ());
    assertEquals ("urn:cen.eu:en16931:2017", aCtx.getCustomizationID ());
    assertEquals ("A1", aCtx.getBusinessProcessType ());
    assertEquals ("FR", aCtx.getSellerVatCountryCode ());
    assertEquals ("FR", aCtx.getSellerPostalCountryCode ());
    assertEquals (EZugferdCountry.FR, aCtx.getResolvedCountry ());
  }

  @Test
  public void testCIICountryResolutionChain ()
  {
    // Seller VAT country wins over the postal country
    assertEquals (EZugferdCountry.FR, _read (_cii ("FR12345678901", "DE", "DE123456789")).getResolvedCountry ());
    // Tax representative VAT country is next
    assertEquals (EZugferdCountry.DE, _read (_cii (null, "FR", "DE123456789")).getResolvedCountry ());
    // Postal country as last resort
    final HybridXmlContext aCtx = _read (_cii (null, "DE", null));
    assertNull (aCtx.getSellerVatCountryCode ());
    assertEquals (EZugferdCountry.DE, aCtx.getResolvedCountry ());
    // Nothing at all
    assertEquals (EZugferdCountry.OTHER, _read (_cii (null, null, null)).getResolvedCountry ());
    // Countries other than DE and FR
    assertEquals (EZugferdCountry.OTHER, _read (_cii ("ATU12345678", "DE", null)).getResolvedCountry ());
    // A VAT number without country prefix falls through to the next source
    final HybridXmlContext aNoPrefix = _read (_cii ("123456789", "DE", null));
    assertNull (aNoPrefix.getSellerVatCountryCode ());
    assertEquals (EZugferdCountry.DE, aNoPrefix.getResolvedCountry ());
    assertEquals (EZugferdCountry.FR, _read (_cii ("123456789", "DE", "FR12345678901")).getResolvedCountry ());
  }

  @Test
  public void testBuyerCountry ()
  {
    final String sCII = "<rsm:CrossIndustryInvoice xmlns:rsm='urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100'" +
                        " xmlns:ram='urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100'>" +
                        "<rsm:SupplyChainTradeTransaction><ram:ApplicableHeaderTradeAgreement><ram:BuyerTradeParty>" +
                        "<ram:PostalTradeAddress><ram:CountryID>DE</ram:CountryID></ram:PostalTradeAddress>%s" +
                        "</ram:BuyerTradeParty></ram:ApplicableHeaderTradeAgreement></rsm:SupplyChainTradeTransaction>" +
                        "</rsm:CrossIndustryInvoice>";
    // Buyer VAT country wins over the postal country
    HybridXmlContext aCtx = _read (String.format (sCII,
                                                  "<ram:SpecifiedTaxRegistration><ram:ID schemeID='VA'>FR12345678901</ram:ID></ram:SpecifiedTaxRegistration>"));
    assertEquals ("FR", aCtx.getBuyerVatCountryCode ());
    assertEquals ("DE", aCtx.getBuyerPostalCountryCode ());
    assertEquals (EZugferdCountry.FR, aCtx.getResolvedBuyerCountry ());
    // Postal country as fallback
    assertEquals (EZugferdCountry.DE, _read (String.format (sCII, "")).getResolvedBuyerCountry ());

    final String sUBL = "<Invoice xmlns='urn:oasis:names:specification:ubl:schema:xsd:Invoice-2'" +
                        " xmlns:cac='urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2'" +
                        " xmlns:cbc='urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2'>" +
                        "<cac:AccountingCustomerParty><cac:Party>" +
                        "<cac:PostalAddress><cac:Country><cbc:IdentificationCode>FR</cbc:IdentificationCode></cac:Country></cac:PostalAddress>" +
                        "<cac:PartyTaxScheme><cbc:CompanyID>DE123456789</cbc:CompanyID><cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme>" +
                        "</cac:Party></cac:AccountingCustomerParty></Invoice>";
    aCtx = _read (sUBL);
    assertEquals (EZugferdCountry.DE, aCtx.getResolvedBuyerCountry ());
    // The buyer never influences the seller country
    assertEquals (EZugferdCountry.OTHER, aCtx.getResolvedCountry ());
  }

  @Test
  public void testUBL ()
  {
    for (final String sRoot : new String [] { "Invoice", "CreditNote" })
    {
      final HybridXmlContext aCtx = _read (_ubl (sRoot, "DE123456789", "DE", null));
      assertEquals (HybridXmlContext.SYNTAX_UBL, aCtx.getSyntax ());
      assertEquals ("urn:cen.eu:en16931:2017#compliant#urn:xeinkauf.de:kosit:xrechnung_3.0",
                    aCtx.getCustomizationID ());
      assertEquals ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0", aCtx.getBusinessProcessType ());
      assertEquals ("DE", aCtx.getSellerVatCountryCode ());
      assertEquals ("DE", aCtx.getSellerPostalCountryCode ());
      assertEquals (EZugferdCountry.DE, aCtx.getResolvedCountry ());
    }
  }

  @Test
  public void testUBLCountryResolutionChain ()
  {
    assertEquals (EZugferdCountry.FR, _read (_ubl ("Invoice", "FR12345678901", "DE", null)).getResolvedCountry ());
    assertEquals (EZugferdCountry.FR, _read (_ubl ("Invoice", null, "DE", "FR12345678901")).getResolvedCountry ());
    assertEquals (EZugferdCountry.FR, _read (_ubl ("Invoice", null, "FR", null)).getResolvedCountry ());
    assertEquals (EZugferdCountry.OTHER, _read (_ubl ("Invoice", null, null, null)).getResolvedCountry ());
    assertEquals (EZugferdCountry.DE, _read (_ubl ("Invoice", "123456789", "DE", null)).getResolvedCountry ());
    // One-Stop-Shop identifiers carry no country and fall through to the next source
    assertEquals (EZugferdCountry.DE, _read (_ubl ("Invoice", "EU372000041", "DE", null)).getResolvedCountry ());
    assertNull (_read (_ubl ("Invoice", "IM2760000001", "DE", null)).getSellerVatCountryCode ());
    // Raw codes are upper-cased
    final HybridXmlContext aLower = _read (_ubl ("Invoice", "de123456789", "de", null));
    assertEquals ("DE", aLower.getSellerVatCountryCode ());
    assertEquals ("DE", aLower.getSellerPostalCountryCode ());
    // Prefixes that are no ISO country code of the issuer fall through to the postal country
    for (final String sVat : new String [] { "XI123456789", "EL123456789", "IN2760000001" })
    {
      final HybridXmlContext aCtx = _read (_ubl ("Invoice", sVat, "DE", null));
      assertNull (sVat, aCtx.getSellerVatCountryCode ());
      assertEquals (sVat, EZugferdCountry.DE, aCtx.getResolvedCountry ());
    }
  }

  @Test
  public void testRootNamespaceIsChecked ()
  {
    assertSame (HybridXmlContext.EMPTY, _read ("<Invoice><CustomizationID>x</CustomizationID></Invoice>"));
    assertSame (HybridXmlContext.EMPTY,
                _read ("<Invoice xmlns='urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2'/>"));
    assertSame (HybridXmlContext.EMPTY, _read ("<CrossIndustryInvoice xmlns='urn:example'/>"));
    assertEquals (HybridXmlContext.SYNTAX_UBL,
                  _read ("<CreditNote xmlns='urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2'/>").getSyntax ());
  }

  @Test
  public void testEmpty ()
  {
    // No XML
    assertSame (HybridXmlContext.EMPTY, HybridXmlContextReader.readXmlContext ((byte []) null));
    // Not well-formed
    assertSame (HybridXmlContext.EMPTY, _read ("<rsm:CrossIndustryInvoice"));
    // Unknown root element (e.g. ZUGFeRD 1.0)
    assertSame (HybridXmlContext.EMPTY,
                _read ("<rsm:CrossIndustryDocument xmlns:rsm='urn:ferd:CrossIndustryDocument:invoice:1p0'/>"));
  }

  @Test
  public void testDoctypeIsRejected ()
  {
    // XXE attempt: DOCTYPE declarations are disallowed, so nothing is read
    final String sXml = "<?xml version='1.0'?>" +
                        "<!DOCTYPE x [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]>" +
                        _cii (null, "&xxe;", null);
    assertSame (HybridXmlContext.EMPTY, _read (sXml));
  }
}
