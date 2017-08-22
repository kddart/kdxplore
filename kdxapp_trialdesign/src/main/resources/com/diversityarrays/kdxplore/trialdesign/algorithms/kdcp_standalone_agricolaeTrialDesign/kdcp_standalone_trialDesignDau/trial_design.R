library('getopt');
library('agricolaeTrialDesign')


spec = matrix(c(
			'outputdir'		, 'out', 0, "character",
			'rns'			, 's', 0, "integer",
			'checksTreatmentfile', 'ct',  0, "character",
			'newTreatmentfile', 'nt',  0, "character",
			'nreps','re',0,"integer",
			'numrows',  'nr', 0, "integer",
			'numcols',  'nc', 0, "integer"


	       ), byrow=TRUE, ncol=4);
opt = getopt(spec);
print(opt)

	if(! is.null(opt$rns)) {
		rns = opt$rns;
	}else{
		rns = 0;
	}

checkTreatments = as.list(read.csv(opt$checksTreatmentfile,header = T))$Treatment;
newTreatments = as.list(read.csv(opt$newTreatmentfile,header = T))$Treatment;

trialDesignDau(checkTreatments,newTreatments,opt$outputdir,opt$numrows,opt$numcols,opt$nreps,rns)
