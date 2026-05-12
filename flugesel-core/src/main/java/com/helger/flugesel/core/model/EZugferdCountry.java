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
package com.helger.flugesel.core.model;

/**
 * Country context used to gate Factur-X / ZUGFeRD country-specific business rules
 * (BR-HYBRID-DE-*, BR-HYBRID-FR-*, BR-FX-DE-*).
 *
 * @author Philip Helger
 */
public enum EZugferdCountry
{
  /** Germany. Activates BR-HYBRID-DE-* and BR-FX-DE-* rules and the PDF/A-3 error downgrade. */
  DE,
  /** France. Activates BR-HYBRID-FR-* rules. */
  FR,
  /** Any other country, or auto-detection failed. Country-specific rules do not fire. */
  OTHER
}
