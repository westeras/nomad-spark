# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure("2") do |config|

  config.vm.box = "ubuntu/xenial64"
  config.vm.box_check_update = false

  config.vm.synced_folder ".", "/usr/local/spark"

  config.vm.provider "virtualbox" do |vb|
    vb.memory = "4096"
  end

  config.vm.provision "dependencies", type: "shell", inline: <<-SHELL
    apt-get update
    apt-get install -y openjdk-8-jdk-headless unzip
    ln -s "$(which python3)" /usr/local/python
  SHELL

  config.vm.provision "docker", type: "shell", inline: <<-SHELL
    curl -fsSL get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
  SHELL

  config.vm.provision "nomad", type: "shell", inline: <<-SHELL
    nomad_version=0.7.0
    wget --no-verbose "https://releases.hashicorp.com/nomad/${nomad_version}/nomad_${nomad_version}_linux_amd64.zip"
    unzip "nomad_${nomad_version}_linux_amd64.zip"
  SHELL

end
