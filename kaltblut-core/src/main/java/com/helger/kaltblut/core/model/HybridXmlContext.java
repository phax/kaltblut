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
package com.helger.kaltblut.core.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.builder.IBuilder;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;

/**
 * Routing-relevant context read from the embedded invoice XML payload (CII or UBL): the BT-24
 * Specification identifier (CustomizationID), the BT-23 Business process type, and the Seller and
 * Buyer countries (VAT-derived and postal) used to resolve the applicable {@link EZugferdCountry}.
 * <p>
 * This is identifier/context extraction, not business-rule validation: no cardinalities, code lists
 * or EN 16931 rules are evaluated. Missing values yield <code>null</code>.
 *
 * @author Alexandre Gabriel Carvalho Silva
 */
@Immutable
public final class HybridXmlContext
{
  /** Syntax identifier for UN/CEFACT Cross Industry Invoice payloads. */
  public static final String SYNTAX_CII = "CII";
  /** Syntax identifier for OASIS UBL payloads. */
  public static final String SYNTAX_UBL = "UBL";

  /** The empty context: no syntax detected, all identifiers absent, country {@code OTHER}. */
  public static final HybridXmlContext EMPTY = builder ().build ();

  private final String m_sSyntax;
  private final String m_sCustomizationID;
  private final String m_sBusinessProcessType;
  private final String m_sSellerVatCountryCode;
  private final String m_sSellerPostalCountryCode;
  private final EZugferdCountry m_eResolvedSellerCountry;
  private final String m_sBuyerVatCountryCode;
  private final String m_sBuyerPostalCountryCode;
  private final EZugferdCountry m_eResolvedBuyerCountry;

  public HybridXmlContext (@Nullable final String sSyntax,
                           @Nullable final String sCustomizationID,
                           @Nullable final String sBusinessProcessType,
                           @Nullable final String sSellerVatCountryCode,
                           @Nullable final String sSellerPostalCountryCode,
                           @NonNull final EZugferdCountry eResolvedSellerCountry,
                           @Nullable final String sBuyerVatCountryCode,
                           @Nullable final String sBuyerPostalCountryCode,
                           @NonNull final EZugferdCountry eResolvedBuyerCountry)
  {
    ValueEnforcer.notNull (eResolvedSellerCountry, "ResolvedSellerCountry");
    ValueEnforcer.notNull (eResolvedBuyerCountry, "ResolvedBuyerCountry");
    m_sSyntax = sSyntax;
    m_sCustomizationID = sCustomizationID;
    m_sBusinessProcessType = sBusinessProcessType;
    m_sSellerVatCountryCode = sSellerVatCountryCode;
    m_sSellerPostalCountryCode = sSellerPostalCountryCode;
    m_eResolvedSellerCountry = eResolvedSellerCountry;
    m_sBuyerVatCountryCode = sBuyerVatCountryCode;
    m_sBuyerPostalCountryCode = sBuyerPostalCountryCode;
    m_eResolvedBuyerCountry = eResolvedBuyerCountry;
  }

  /**
   * @return {@link #SYNTAX_CII} or {@link #SYNTAX_UBL}; the detected payload syntax, or
   *         <code>null</code> if unknown.
   */
  @Nullable
  public String getSyntax ()
  {
    return m_sSyntax;
  }

  /**
   * @return the BT-24 Specification identifier, or <code>null</code>.
   */
  @Nullable
  public String getCustomizationID ()
  {
    return m_sCustomizationID;
  }

  /**
   * @return the BT-23 Business process type, or <code>null</code>.
   */
  @Nullable
  public String getBusinessProcessType ()
  {
    return m_sBusinessProcessType;
  }

  /**
   * @return the seller country derived from the VAT identifier prefix (first 2 chars), or
   *         <code>null</code>.
   */
  @Nullable
  public String getSellerVatCountryCode ()
  {
    return m_sSellerVatCountryCode;
  }

  /**
   * @return the raw seller postal country code (BT-40), or <code>null</code>.
   */
  @Nullable
  public String getSellerPostalCountryCode ()
  {
    return m_sSellerPostalCountryCode;
  }

  /**
   * @return the seller jurisdiction resolved via the Peppol VAT-prefix-first chain (seller VAT,
   *         then tax representative VAT, then seller postal country); never <code>null</code>.
   *         {@link EZugferdCountry#OTHER} when absent or unmapped.
   */
  @NonNull
  public EZugferdCountry getResolvedSellerCountry ()
  {
    return m_eResolvedSellerCountry;
  }

  /**
   * @return the buyer country derived from the VAT identifier prefix (first 2 chars), or
   *         <code>null</code>.
   */
  @Nullable
  public String getBuyerVatCountryCode ()
  {
    return m_sBuyerVatCountryCode;
  }

  /**
   * @return the raw buyer postal country code (BT-55), or <code>null</code>.
   */
  @Nullable
  public String getBuyerPostalCountryCode ()
  {
    return m_sBuyerPostalCountryCode;
  }

  /**
   * @return the raw buyer country code via the VAT-prefix-first chain (buyer VAT prefix, then buyer
   *         postal country), or <code>null</code> if neither is present.
   */
  @Nullable
  public String getBuyerCountryCode ()
  {
    return m_sBuyerVatCountryCode != null ? m_sBuyerVatCountryCode : m_sBuyerPostalCountryCode;
  }

  /**
   * @return the buyer jurisdiction resolved via the Peppol VAT-prefix-first chain (buyer VAT, then
   *         buyer postal country); never <code>null</code>. {@link EZugferdCountry#OTHER} when
   *         absent or unmapped.
   */
  @NonNull
  public EZugferdCountry getResolvedBuyerCountry ()
  {
    return m_eResolvedBuyerCountry;
  }

  @Override
  @NonNull
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Syntax", m_sSyntax)
                                       .append ("CustomizationID", m_sCustomizationID)
                                       .append ("BusinessProcessType", m_sBusinessProcessType)
                                       .append ("SellerVatCountryCode", m_sSellerVatCountryCode)
                                       .append ("SellerPostalCountryCode", m_sSellerPostalCountryCode)
                                       .append ("ResolvedSellerCountry", m_eResolvedSellerCountry)
                                       .append ("BuyerVatCountryCode", m_sBuyerVatCountryCode)
                                       .append ("BuyerPostalCountryCode", m_sBuyerPostalCountryCode)
                                       .append ("ResolvedBuyerCountry", m_eResolvedBuyerCountry)
                                       .getToString ();
  }

  /**
   * Builder for {@link HybridXmlContext}.
   *
   * @author Alexandre Gabriel Carvalho Silva
   */
  @NotThreadSafe
  public static final class Builder implements IBuilder <HybridXmlContext>
  {
    private String m_sSyntax;
    private String m_sCustomizationID;
    private String m_sBusinessProcessType;
    private String m_sSellerVatCountryCode;
    private String m_sSellerPostalCountryCode;
    private EZugferdCountry m_eResolvedSellerCountry = EZugferdCountry.OTHER;
    private String m_sBuyerVatCountryCode;
    private String m_sBuyerPostalCountryCode;
    private EZugferdCountry m_eResolvedBuyerCountry = EZugferdCountry.OTHER;

    Builder ()
    {}

    /**
     * @param sSyntax
     *        the payload syntax, {@link #SYNTAX_CII} or {@link #SYNTAX_UBL}. May be
     *        <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public Builder syntax (@Nullable final String sSyntax)
    {
      m_sSyntax = sSyntax;
      return this;
    }

    /**
     * @param sCustomizationID
     *        the BT-24 Specification identifier. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public Builder customizationID (@Nullable final String sCustomizationID)
    {
      m_sCustomizationID = sCustomizationID;
      return this;
    }

    /**
     * @param sBusinessProcessType
     *        the BT-23 Business process type. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public Builder businessProcessType (@Nullable final String sBusinessProcessType)
    {
      m_sBusinessProcessType = sBusinessProcessType;
      return this;
    }

    /**
     * @param sSellerVatCountryCode
     *        the seller VAT identifier country prefix. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public Builder sellerVatCountryCode (@Nullable final String sSellerVatCountryCode)
    {
      m_sSellerVatCountryCode = sSellerVatCountryCode;
      return this;
    }

    /**
     * @param sSellerPostalCountryCode
     *        the seller postal country code (BT-40). May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public Builder sellerPostalCountryCode (@Nullable final String sSellerPostalCountryCode)
    {
      m_sSellerPostalCountryCode = sSellerPostalCountryCode;
      return this;
    }

    /**
     * @param eResolvedSellerCountry
     *        the resolved seller country. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public Builder resolvedSellerCountry (@NonNull final EZugferdCountry eResolvedSellerCountry)
    {
      ValueEnforcer.notNull (eResolvedSellerCountry, "ResolvedSellerCountry");
      m_eResolvedSellerCountry = eResolvedSellerCountry;
      return this;
    }

    /**
     * @param sBuyerVatCountryCode
     *        the buyer VAT identifier country prefix. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public Builder buyerVatCountryCode (@Nullable final String sBuyerVatCountryCode)
    {
      m_sBuyerVatCountryCode = sBuyerVatCountryCode;
      return this;
    }

    /**
     * @param sBuyerPostalCountryCode
     *        the buyer postal country code (BT-55). May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public Builder buyerPostalCountryCode (@Nullable final String sBuyerPostalCountryCode)
    {
      m_sBuyerPostalCountryCode = sBuyerPostalCountryCode;
      return this;
    }

    /**
     * @param eResolvedBuyerCountry
     *        the resolved buyer country. May not be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public Builder resolvedBuyerCountry (@NonNull final EZugferdCountry eResolvedBuyerCountry)
    {
      ValueEnforcer.notNull (eResolvedBuyerCountry, "ResolvedBuyerCountry");
      m_eResolvedBuyerCountry = eResolvedBuyerCountry;
      return this;
    }

    @NonNull
    public HybridXmlContext build ()
    {
      return new HybridXmlContext (m_sSyntax,
                                   m_sCustomizationID,
                                   m_sBusinessProcessType,
                                   m_sSellerVatCountryCode,
                                   m_sSellerPostalCountryCode,
                                   m_eResolvedSellerCountry,
                                   m_sBuyerVatCountryCode,
                                   m_sBuyerPostalCountryCode,
                                   m_eResolvedBuyerCountry);
    }
  }

  /**
   * @return a new builder. The resolved countries default to {@link EZugferdCountry#OTHER}.
   */
  @NonNull
  public static Builder builder ()
  {
    return new Builder ();
  }
}
