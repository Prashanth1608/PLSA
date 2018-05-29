
import org.kohsuke.args4j.Option;

public class CmdArgs {

	@Option(name = "-corpus", usage = "Specify path to topic modeling corpus")
	public String corpus = "";

	@Option(name = "-ntopics", usage = "Specify number of topics")
	public int ntopics = 20;

	@Option(name = "-niters", usage = "Specify number of EM-style sampling iterations")
	public int niters = 1000;

	@Option(name = "-name", usage = "Specify a (string) name to an PLSA output (e.g. test)")
	public String expModelName = "model";

}
