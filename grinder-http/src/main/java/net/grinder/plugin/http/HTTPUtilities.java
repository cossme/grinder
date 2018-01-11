// Copyright (C) 2005 - 2009 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.plugin.http;

import java.util.List;

import net.grinder.common.GrinderException;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;


/**
 * Things that HTTP scripts find useful.
 *
 * @author Philip Aston
 * @see HTTPPluginControl#getHTTPUtilities()
 */
public interface HTTPUtilities {

  /**
   * Create a {@link NVPair} for an HTTP Basic Authorization header.
   *
   * @param userID
   *          The user name.
   * @param password
   *          The password.
   * @return The NVPair that can be used as a header with {@link HTTPRequest}.
   */
  NVPair basicAuthorizationHeader(String userID, String password);

  /**
   * Return the response for the last request made by the calling worker thread.
   *
   * @return The response, or {@code null} if the calling thread has not
   *         made any requests.
   * @throws GrinderException
   *           If not called from a worker thread.
   */
  HTTPResponse getLastResponse() throws GrinderException;

  /**
   * Return the value for a path parameter or query string name-value token with
   * the given {@code tokenName} in a Location header from the last
   * response. If there are multiple matches, the first value is returned.
   *
   * <p>
   * If there is no match, an empty string is returned rather than
   * {@code null}. This makes scripts more robust (as they don't need to
   * check the value before using it), but they lose the ability to distinguish
   * between a missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @return The first value if one is found.
   * @throws GrinderException
   *           If not called from a worker thread.
   */
  String valueFromLocationURI(String tokenName) throws GrinderException;


  /**
   * Return the value for an input token with the given {@code tokenName} in the
   * body of the last response. If there are multiple matches, the first value
   * is returned.
   *
   * <p>
   * If there is no match, an empty string is returned rather than {@code null}.
   * This makes scripts more robust (as they don't need to check the value
   * before using it), but they lose the ability to distinguish between a
   * missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @return The first value if one is found, or an empty string.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valueFromBodyInput(String, String)
   * @see #valuesFromBodyInput(String)
   */
  String valueFromBodyInput(String tokenName) throws GrinderException;

  /**
   * Return the value for an input token with the given {@code tokenName} in the
   * body of the last response. If there are multiple matches, the first value
   * is returned. This version of {@code valueFromBodyInput} only considers
   * matches following the first occurrence of the literal text {@code
   * afterText}. If there are multiple matches, the first value is returned.
   *
   * <p>
   * If there is no match, an empty string is returned rather than {@code null}.
   * This makes scripts more robust (as they don't need to check the value
   * before using it), but they lose the ability to distinguish between a
   * missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @param afterText
   *          The search begins after the first occurrence of this literal text.
   * @return The first value if one is found, or an empty string if the body
   *         does not contain {@code afterText} followed by a URI containing a
   *         token with name {@code tokenName}.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valueFromBodyInput(String)
   * @see #valuesFromBodyInput(String, String)
   */
  String valueFromBodyInput(String tokenName, String afterText)
    throws GrinderException;

  /**
   * Return all matching values for input tokens with the given {@code
   * tokenName} in the body of the last response.
   *
   * @param tokenName
   *          The token name.
   * @return The matching values.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valueFromBodyInput(String)
   * @see #valuesFromBodyInput(String, String)
   */
  List<String> valuesFromBodyInput(String tokenName) throws GrinderException;

  /**
   * Return all matching values for input tokens with the given {@code
   * tokenName} in the body of the last response. This version of {@code
   * valueFromBodyInput} only considers matches following the first occurrence
   * of the literal text {@code afterText}.
   *
   * @param tokenName
   *          The token name.
   * @param afterText
   *          The search begins after the first occurrence of this literal text.
   * @return The matching values.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valuesFromBodyInput(String)
   * @see #valueFromBodyInput(String, String)
   */
  List<String> valuesFromBodyInput(String tokenName, String afterText)
    throws GrinderException;

  /**
   * Return the value for a hidden input token with the given
   * {@code tokenName} in the body of the last response. If there are
   * multiple matches, the first value is returned.
   *
   * <p>
   * If there is no match, an empty string is returned rather than
   * {@code null}. This makes scripts more robust (as they don't need to
   * check the value before using it), but they lose the ability to distinguish
   * between a missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @return The first value if one is found, or an empty string.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valueFromHiddenInput(String, String)
   * @see #valuesFromHiddenInput(String)
   */
  String valueFromHiddenInput(String tokenName) throws GrinderException;

  /**
   * Return the value for a hidden input token with the given
   * {@code tokenName} in the body of the last response. If there are
   * multiple matches, the first value is returned. This version of
   * {@code valueFromHiddenInput} only considers matches following the
   * first occurrence of the literal text {@code afterText}. If there
   * are multiple matches, the first value is returned.
   *
   * <p>
   * If there is no match, an empty string is returned rather than
   * {@code null}. This makes scripts more robust (as they don't need to
   * check the value before using it), but they lose the ability to distinguish
   * between a missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @param afterText
   *          The search begins after the first occurrence of this literal text.
   * @return The first value if one is found, or an empty string if the body
   *         does not contain {@code afterText} followed by a URI
   *         containing a token with name {@code tokenName}.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valueFromHiddenInput(String)
   * @see #valuesFromHiddenInput(String, String)
   */
  String valueFromHiddenInput(String tokenName, String afterText)
    throws GrinderException;

  /**
   * Return all matching values for hidden input tokens with the given
   * {@code tokenName} in the body of the last response.
   *
   * @param tokenName
   *          The token name.
   * @return The matching values.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valueFromHiddenInput(String)
   * @see #valuesFromHiddenInput(String, String)
   */
  List<String> valuesFromHiddenInput(String tokenName) throws GrinderException;

  /**
   * Return all matching values for hidden input tokens with the given
   * {@code tokenName} in the body of the last response. This version of
   * {@code valueFromHiddenInput} only considers matches following the
   * first occurrence of the literal text {@code afterText}.
   *
   * @param tokenName
   *          The token name.
   * @param afterText
   *          The search begins after the first occurrence of this literal text.
   * @return The matching values.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valuesFromHiddenInput(String)
   * @see #valueFromHiddenInput(String, String)
   */
  List<String> valuesFromHiddenInput(String tokenName, String afterText)
    throws GrinderException;

  /**
   * Return the value for a path parameter or query string name-value token with
   * the given {@code tokenName} in a URI in the body of the last
   * response. If there are multiple matches, the first value is returned.
   *
   * <p>
   * If there is no match, an empty string is returned rather than
   * {@code null}. This makes scripts more robust (as they don't need to
   * check the value before using it), but they lose the ability to distinguish
   * between a missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @return The first value if one is found, or an empty string.
   * @throws GrinderException If not called from a worker thread.
   * @see #valueFromBodyURI(String, String)
   * @see #valuesFromBodyURI(String)
   */
  String valueFromBodyURI(String tokenName) throws GrinderException;

  /**
   * Return the value for a path parameter or query string name-value token with
   * the given {@code tokenName} in a URI in the body of the last
   * response. This version of {@code valueFromBodyURI} only considers
   * matches following the first occurrence of the literal text
   * {@code afterText}. If there are multiple matches, the first value
   * is returned.
   *
   * <p>
   * If there is no match, an empty string is returned rather than
   * {@code null}. This makes scripts more robust (as they don't need to
   * check the value before using it), but they lose the ability to distinguish
   * between a missing token and an empty value.
   * </p>
   *
   * @param tokenName
   *          The token name.
   * @param afterText
   *          The search begins after the first occurrence of this literal text.
   * @return The first value if one is found, or an empty string if the body
   *         does not contain {@code afterText} followed by a URI
   *         containing a token with name {@code tokenName}.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valueFromBodyURI(String)
   * @see #valuesFromBodyURI(String, String)
   */
  String valueFromBodyURI(String tokenName, String afterText)
    throws GrinderException;

  /**
   * Return all matching values for path parameters or query string name-value
   * tokens with the given {@code tokenName} in a URI in the body of the
   * last response.
   *
   * @param tokenName
   *          The token name.
   * @return The matching values.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valueFromBodyURI(String)
   * @see #valuesFromBodyURI(String, String)
   */
  List<String> valuesFromBodyURI(String tokenName) throws GrinderException;

  /**
   * Return all matching values for path parameters or query string name-value
   * tokens with the given {@code tokenName} in a URI in the body of the
   * last response. This version of {@code valueFromBodyURI} only considers
   * matches following the first occurrence of the literal text
   * {@code afterText}.
   *
   * @param tokenName
   *          The token name.
   * @param afterText
   *          The search begins after the first occurrence of this literal text.
   * @return The matching values.
   * @throws GrinderException
   *           If not called from a worker thread.
   * @see #valuesFromBodyURI(String)
   * @see #valueFromBodyURI(String, String)
   */
  List<String> valuesFromBodyURI(String tokenName, String afterText)
    throws GrinderException;
}
