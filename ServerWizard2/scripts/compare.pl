use strict;
use XML::Simple;
use Data::Dumper;

$XML::Simple::PREFERRED_PARSER = 'XML::Parser';
my $xml_gold = XMLin("C:\\Users\\IBM_ADMIN\\Documents\\workspace.juno\\ServerWizard2\\xml\\FIRESTONE_hb.mrw.ref.xml");
my $xml_new = XMLin("C:\\Users\\IBM_ADMIN\\Documents\\workspace.juno\\ServerWizard2\\xml\\FIRESTONE_hb.mrw.xml");

#print Dumper($xml_gold);
my % attr_values;
foreach my $id (sort (keys %{$xml_new->{targetInstance}})) {
	my $aff_path=$xml_new->{targetInstance}->{$id}->{attribute}->{AFFINITY_PATH}->{default};
	foreach my $attr (sort (keys %{$xml_new->{targetInstance}->{$id}->{attribute}})) {
		if (ref($xml_new->{targetInstance}->{$id}->{attribute}->{$attr}->{default}) eq "HASH") {
			if (defined($xml_new->{targetInstance}->{$id}->{attribute}->{$attr}->{default}->{field})) {
				foreach my $field (sort (keys %{$xml_new->{targetInstance}->{$id}->{attribute}->{$attr}->{default}->{field}})) {
					my $value=$xml_new->{targetInstance}->{$id}->{attribute}->{$attr}->{default}->{field}->{$field}->{value};
					my $key=$aff_path." | ".$attr."_".$field;
					$value=~s/\t//g;
					$value=~s/\s//g;
					$attr_values{$key}=$value;
				}
			}
		} else {
			my $value=$xml_new->{targetInstance}->{$id}->{attribute}->{$attr}->{default};
			my $key=$aff_path." | ".$attr;
			$value=~s/\t//g;
			$value=~s/\s//g;

			$attr_values{$key}=$value;
			#print ">>>> $key = $value\n";
		}
	}
}


foreach my $id (sort (keys %{$xml_gold->{targetInstance}})) {
	my $aff_path=$xml_gold->{targetInstance}->{$id}->{attribute}->{AFFINITY_PATH}->{default};

	foreach my $attr (sort (keys %{$xml_gold->{targetInstance}->{$id}->{attribute}})) {
		
		if (ref($xml_gold->{targetInstance}->{$id}->{attribute}->{$attr}->{default}) eq "HASH") {
			if (defined($xml_gold->{targetInstance}->{$id}->{attribute}->{$attr}->{default}->{field})) {
				foreach my $field (sort (keys %{$xml_gold->{targetInstance}->{$id}->{attribute}->{$attr}->{default}->{field}})) {
					my $value=$xml_gold->{targetInstance}->{$id}->{attribute}->{$attr}->{default}->{field}->{$field}->{value};
					$value=~s/\t//g;
					$value=~s/\s//g;
					
					my $key=$aff_path." | ".$attr."_".$field;
					my $newval=$attr_values{$key};
					if ($value ne $newval) {
						print "$key | $value != $newval\n";
					}
					if ($key=~/PHYS_PATH/) { 
					#	print "$key = $value\n";
					}
				}
			}
		} else {
			my $value=$xml_gold->{targetInstance}->{$id}->{attribute}->{$attr}->{default};
			$value=~s/\t//g;
			$value=~s/\s//g;
			
			my $key=$aff_path." | ".$attr;
			my $newval=$attr_values{$key};
			if ($value ne $newval) {
				print "$key | $value != $newval\n";
			}
			if ($key=~/PHYS_PATH/) { 
				#print "$key = $value\n";
			}
		}
	}
}

