import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-author',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './author.component.html',
  styleUrl: './author.component.css'
})
export class AuthorComponent {
  skills = [
    { name: 'Java 17+', category: 'Languages', level: 95 },
    { name: 'Spring Boot', category: 'Frameworks', level: 98 },
    { name: 'Kafka', category: 'Streaming', level: 92 },
    { name: 'Kubernetes', category: 'Cloud-Native', level: 90 },
    { name: 'AWS/Azure', category: 'Public Cloud', level: 88 },
    { name: 'Python (AI/ML)', category: 'Analytics', level: 85 },
    { name: 'Microservices', category: 'Architecture', level: 99 },
    { name: 'OpenShift', category: 'Orchestration', level: 85 }
  ];

  experience = [
    {
      company: 'Citi Group',
      role: 'Technology Lead / Senior Staff Engineer',
      period: '2017 – Present',
      desc: 'Architecting cloud-native platforms for global wholesale lending and counterparty credit risk. Leading distributed systems migration and Kafka-driven event architectures.',
      tags: ['Java 17', 'Kafka', 'Kubernetes', 'AIOps']
    },
    {
      company: 'Apple Inc.',
      role: 'Senior Software Engineer',
      period: '2015 – 2016',
      desc: 'Engineered core REST services for the global Apple Retail POS platform. Optimized high-availability transaction processing and payment workflows.',
      tags: ['Java', 'Spring Boot', 'Oracle', 'RabbitMQ']
    },
    {
      company: 'Philips',
      role: 'Lead Software Engineer',
      period: '2014 – 2015',
      desc: 'Built a cloud-based digital health platform for real-time device data integration using Cloud Foundry.',
      tags: ['Java', 'Spring', 'Cloud Foundry', 'PaaS']
    },
    {
      company: 'Cisco',
      role: 'Software Engineer',
      period: '2012 – 2014',
      desc: 'Developed cloud automation and orchestration modules for private/hybrid cloud provisioning.',
      tags: ['Java', 'Apache CXF', 'ActiveMQ', 'Performance']
    }
  ];

  specializations = [
    { title: 'Microservices Architecture', icon: '⚡', desc: 'Designing high-throughput, resilient distributed systems at global banking scale.' },
    { title: 'Cloud & Kubernetes', icon: '☁️', desc: 'Expertise in AWS, Azure, and OpenShift for mission-critical enterprise workloads.' },
    { title: 'AI & Observability', icon: '🤖', desc: 'Integrating ML models for risk analytics and AIOps for self-healing systems.' }
  ];
}
