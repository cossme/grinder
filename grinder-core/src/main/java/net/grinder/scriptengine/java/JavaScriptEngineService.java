package net.grinder.scriptengine.java;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.List;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.DCRContext;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.ScriptEngineService;


/**
 * Java {@link ScriptEngineService} implementation.
 *
 * @author Philip Aston
 */
public final class JavaScriptEngineService implements ScriptEngineService {

  private final DCRContext m_dcrContext;

  /**
   * Constructor.
   *
   * @param dcrContext DCR context.
   */
  public JavaScriptEngineService(DCRContext dcrContext) {
    m_dcrContext = dcrContext;
  }

  /**
   * Constructor used when DCR is unavailable.
   */
  public JavaScriptEngineService() {
    this(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override public List<? extends Instrumenter> createInstrumenters()
    throws EngineException {

    if (m_dcrContext != null) {
      return asList(new JavaDCRInstrumenter(m_dcrContext));
    }

    return emptyList();
  }

  /**
   * {@inheritDoc}
   */
  @Override public ScriptEngine createScriptEngine(ScriptLocation script)
    throws EngineException {
    return null;
  }
}
